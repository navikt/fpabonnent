package no.nav.foreldrepenger.abonnent.feed.tps;

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
import no.nav.foreldrepenger.abonnent.pdl.PdlFeatureToggleTjeneste;
import no.nav.tjenester.person.feed.common.v1.Feed;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.Meldingstype;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class TpsFeedPoller implements FeedPoller {
    private static final Logger log = LoggerFactory.getLogger(TpsFeedPoller.class);

    private static final String ENDPOINT_KEY = "person.feed.v2.url";
    private static final String POLLING_AKTIVERT_KEY = "personfeed.polling.aktivert";
    private static final String POLLING_AKTIVERT_VALUE = "aktiv";
    private static final String POLLING_AKTIVERT_DEFAULT = "false";
    private static final String PAGE_SIZE_VALUE_KEY = "feed.pagesize.value";
    private static final String PAGE_SIZE_PARAM = "pageSize";
    private static final String SEQUENCE_ID_PARAM = "sequenceId";

    private static final String DEAKTIVERT_LOG = "TPS polling er deaktivert";
    private static final String AKTIVERT_LOG = "TPS polling er aktivert";

    private static Set<String> AKSEPTERTE_MELDINGSTYPER = Stream.of(Meldingstype.FOEDSELSMELDINGOPPRETTET,
            Meldingstype.DOEDSMELDINGOPPRETTET, Meldingstype.DOEDFOEDSELOPPRETTET)
            .map(Meldingstype::name).collect(Collectors.toSet());

    private URI endpoint;
    private HendelseRepository hendelseRepository;
    private OidcRestClient oidcRestClient;
    private String pageSize;
    private boolean pollingErAktivert;
    private PdlLanseringTjeneste pdlLanseringTjeneste;
    private PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste;

    @Inject
    public TpsFeedPoller(@KonfigVerdi(ENDPOINT_KEY) URI endpoint,
                         HendelseRepository hendelseRepository,
                         OidcRestClient oidcRestClient,
                         @KonfigVerdi(value = PAGE_SIZE_VALUE_KEY, defaultVerdi = KonfigVerdier.PAGE_SIZE_VALUE_DEFAULT) String pageSize,
                         @KonfigVerdi(value = POLLING_AKTIVERT_KEY, defaultVerdi = POLLING_AKTIVERT_DEFAULT) String pollingErAktivert,
                         PdlLanseringTjeneste pdlLanseringTjeneste,
                         PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste) {
        this.endpoint = endpoint;
        this.hendelseRepository = hendelseRepository;
        this.oidcRestClient = oidcRestClient;
        this.pageSize = pageSize;
        this.pollingErAktivert = POLLING_AKTIVERT_VALUE.equalsIgnoreCase(pollingErAktivert);
        if (this.pollingErAktivert) {
            log.info(AKTIVERT_LOG);
        } else {
            log.info(DEAKTIVERT_LOG);
        }
        this.pdlLanseringTjeneste = pdlLanseringTjeneste;
        this.pdlFeatureToggleTjeneste = pdlFeatureToggleTjeneste;
    }

    @Override
    public FeedKode getFeedKode() {
        return FeedKode.TPS;
    }

    @Override
    public void poll(InputFeed inputFeed) {
        if (!pollingErAktivert) {
            log.debug(DEAKTIVERT_LOG);
            return;
        }

        log.info("Polling TPS, inputFeed: {}", inputFeed); // NOSONAR
        String pollId = UUID.randomUUID().toString();

        URI request = request(inputFeed);

        Feed personFeed = oidcRestClient.get(request, Feed.class);

        if (personFeed == null) {
            log.warn("Kunne ikke hente tpsFeed for endpoint={}", request); // NOSONAR
            inputFeed.oppdaterFeilet();
        } else if (personFeed.getItems() != null && !personFeed.getItems().isEmpty()) {

            Optional<Long> lastSequenceId = Optional.empty();
            for (FeedEntry entry : personFeed.getItems()) {
                if (AKSEPTERTE_MELDINGSTYPER.contains(entry.getType())) {
                    Optional<String> pdlHendelseId;
                    if (pdlFeatureToggleTjeneste.skalSendePdlOgDuplikatsjekkePf()) {
                        pdlHendelseId = pdlLanseringTjeneste.sjekkOmTpsHendelseErMottattFraPdlAllerede(entry);
                    } else {
                        pdlHendelseId = Optional.empty();
                    }
                    if (pdlHendelseId.isPresent()) {
                        log.info("Hendelse med sekvensnummer {} av type {} er allerede mottatt fra PDL som ID {} og vil ikke bli sortert", entry.getSequence(), entry.getType(), pdlHendelseId.get());
                        lagreInngåendeHendelse(entry, pollId, HåndtertStatusType.HÅNDTERT);
                    } else {
                        lagreInngåendeHendelse(entry, pollId, HåndtertStatusType.MOTTATT);
                    }
                }
                lastSequenceId = Optional.of(entry.getSequence());
            }

            lastSequenceId.ifPresent(aLong -> inputFeed.oppdaterLestOk(SEQUENCE_ID_PARAM + "=" + (aLong + 1)));
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
            return 1L;
        }
    }

    private void lagreInngåendeHendelse(FeedEntry entry, String pollId, HåndtertStatusType håndtertStatus) {
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
                .hendelseId("" + entry.getSequence())
                .type(HendelseType.fraKodeDefaultUdefinert(entry.getType()))
                .payload(JsonMapper.toJson(entry))
                .feedKode(FeedKode.TPS)
                .requestUuid(pollId)
                .håndtertStatus(håndtertStatus)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
    }
}
