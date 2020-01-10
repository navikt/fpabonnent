package no.nav.foreldrepenger.abonnent.feed.infotrygd;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.feed.poller.FeedPoller;
import no.nav.foreldrepenger.abonnent.feed.poller.FeedPollerFeil;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.konfig.KonfigVerdier;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdFeedPoller implements FeedPoller {
    private static final String ENDPOINT_KEY = "infotrygd.hendelser.api.url";
    private static final String PAGE_SIZE_VALUE_KEY = "feed.pagesize.value";
    private static final String POLLING_AKTIVERT_KEY = "infotrygdfeed.polling.aktivert";
    private static final String POLLING_AKTIVERT_VALUE = "aktiv";
    private static final String FORSINKELSE_MINUTTER_KEY = "infotrygd.hendelser.forsinkelse.minutter";

    private static final String PAGE_SIZE_PARAM = "maxAntall";
    private static final String SEQUENCE_ID_PARAM = "sistLesteSekvensId";

    private static final Logger log = LoggerFactory.getLogger(InfotrygdFeedPoller.class);

    private static final String DEAKTIVERT_LOG = "Infotrygd polling er deaktivert";

    private static Set<String> AKSEPTERTE_MELDINGSTYPER = Stream.of(Meldingstype.INFOTRYGD__OPPHOERT,
            Meldingstype.INFOTRYGD_ANNULLERT, Meldingstype.INFOTRYGD_INNVILGET)
            .map(Meldingstype::getType).collect(Collectors.toSet());

    private URI endpoint;
    private String pageSize;
    private boolean pollingErAktivert;
    private Integer forsinkelseMinutter;

    private HendelseRepository hendelseRepository;
    private OidcRestClient oidcRestClient;

    @Inject
    public InfotrygdFeedPoller(@KonfigVerdi(ENDPOINT_KEY) URI endpoint,
                               HendelseRepository hendelseRepository,
                               OidcRestClient oidcRestClient,
                               @KonfigVerdi(value = PAGE_SIZE_VALUE_KEY, defaultVerdi = KonfigVerdier.PAGE_SIZE_VALUE_DEFAULT) String pageSize,
                               @KonfigVerdi(value = POLLING_AKTIVERT_KEY, defaultVerdi = KonfigVerdier.INFOTRYGDFEED_POLLING_AKTIVERT_DEFAULT) String pollingErAktivert,
                               @KonfigVerdi(value = FORSINKELSE_MINUTTER_KEY, defaultVerdi = KonfigVerdier.INFOTRYGD_HENDELSER_FORSINKELSE_MINUTTER_DEFAULT) Integer forsinkelseMinutter) {
        this.endpoint = endpoint;
        this.hendelseRepository = hendelseRepository;
        this.oidcRestClient = oidcRestClient;
        this.pageSize = pageSize;
        this.pollingErAktivert = POLLING_AKTIVERT_VALUE.equalsIgnoreCase(pollingErAktivert);
        this.forsinkelseMinutter = forsinkelseMinutter;
        if (!this.pollingErAktivert) {
            log.info(DEAKTIVERT_LOG);
        }
    }

    @Override
    public FeedKode getFeedKode() {
        return FeedKode.INFOTRYGD;
    }

    @Override
    public void poll(InputFeed inputFeed) {
        if (!pollingErAktivert) {
            log.debug(DEAKTIVERT_LOG);
            return;
        }

        log.info("Polling Infotrygd, inputFeed: {}", inputFeed); // NOSONAR
        String pollId = UUID.randomUUID().toString();

        URI request = request(inputFeed);

        FeedDto infoTrygdFeed = oidcRestClient.get(request, FeedDto.class);

        log.debug("Fått Response fra Infotrygd");

        if (infoTrygdFeed == null) {
            log.warn("Kunne ikke hente infoTrygdFeed for endpoint={}", request); // NOSONAR
            inputFeed.oppdaterFeilet();
        } else if (infoTrygdFeed.getElementer() != null && !infoTrygdFeed.getElementer().isEmpty()) {

            Optional<Long> lastSequenceId = Optional.empty();

            for (FeedElement feedElement : infoTrygdFeed.getElementer()) {
                log.info("leser Infotrygdhendelse med sekvensId {} og koblingId {}", feedElement.getSekvensId(), feedElement.getKoblingId());
                if (AKSEPTERTE_MELDINGSTYPER.contains(feedElement.getType())) {
                    log.info("lagrer Infotrygdhendelse: {}, {}", feedElement.getSekvensId(), feedElement.getType());
                    lagreInngåendeInfotrygdMelding(feedElement, pollId);
                } else {
                    log.info("Ignorerer Infotrygdhendelse med sekvensId {} og type {}", feedElement.getSekvensId(), feedElement.getType());
                }
                lastSequenceId = Optional.of(feedElement.getSekvensId());
            }

            if (lastSequenceId.isPresent()) {
                inputFeed.oppdaterLestOk(SEQUENCE_ID_PARAM + "=" + (lastSequenceId.get()));
                log.info("lest Infotrygdhendelser opp til sekvensId {}", lastSequenceId.get());
            } else {
                log.info("Meldinger ikke lest");
            }
        } else {
            inputFeed.oppdaterLestOk(inputFeed.getNextUrl().orElse(null));
        }
    }

    @Override
    public URI request(InputFeed inputFeed) {
        Long sequenceId = finnStartSequenceId(inputFeed.getNextUrl());
        try {
            return new URIBuilder(endpoint)
                    .addParameter(SEQUENCE_ID_PARAM, String.valueOf(sequenceId))
                    .addParameter(PAGE_SIZE_PARAM, pageSize).build();
        } catch (URISyntaxException e) {
            throw FeedPollerFeil.FACTORY.kanIkkeUtledeNextUrl(e).toException();
        }
    }

    private Long finnStartSequenceId(Optional<String> nextUrl) {
        if (nextUrl.isPresent() && nextUrl.get().contains(SEQUENCE_ID_PARAM)) {
            return Long.parseLong(nextUrl.get().replace(SEQUENCE_ID_PARAM + "=", ""));
        } else {
            return 0L;
        }
    }

    private void lagreInngåendeInfotrygdMelding(FeedElement feedElement, String pollId) {
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
                .sekvensnummer(feedElement.getSekvensId())
                .koblingId(feedElement.getKoblingId())
                .type(HendelseType.fraKodeDefaultUdefinert(feedElement.getType()))
                .payload(JsonMapper.toJson(feedElement))
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid(pollId)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now().plusMinutes(forsinkelseMinutter))
                .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
    }
}
