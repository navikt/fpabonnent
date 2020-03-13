package no.nav.foreldrepenger.abonnent.tjenester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.feed.domain.DødHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.DødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.feed.tps.DødfødselOpprettetHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.feed.tps.DødsmeldingOpprettetHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.feed.tps.FødselsmeldingOpprettetHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.feed.tps.TpsFeedPoller;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.tjenester.person.feed.common.v1.Feed;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class TpsFeedPollerOgInngåendeHendelseTest {
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }
    private static final String BASE_URL_FEED = "http://foo.bar/tpsfeed";
    private URI startUri = URI.create(BASE_URL_FEED + "?sequenceId=1&pageSize=5");
    private URI endpoint = URI.create(BASE_URL_FEED);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());

    @Mock
    private OidcRestClient oidcRestClient;

    private TpsFeedPoller poller;
    @Mock
    private InputFeed inputFeed;

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Before
    public void setUp() {
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste fødselHendelseTjeneste = new FødselsmeldingOpprettetHendelseTjeneste();
        HendelseTjeneste dødHendelseTjeneste = new DødsmeldingOpprettetHendelseTjeneste();
        HendelseTjeneste dødfødselHendelseTjeneste = new DødfødselOpprettetHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(eq(HendelseType.FØDSELSMELDINGOPPRETTET), anyLong()))
                .thenReturn(fødselHendelseTjeneste);
        when(hendelseTjenesteProvider.finnTjeneste(eq(HendelseType.DØDSMELDINGOPPRETTET), anyLong()))
                .thenReturn(dødHendelseTjeneste);
        when(hendelseTjenesteProvider.finnTjeneste(eq(HendelseType.DØDFØDSELOPPRETTET), anyLong()))
                .thenReturn(dødfødselHendelseTjeneste);

        poller = new TpsFeedPoller(endpoint, hendelseRepository, oidcRestClient, "5", "aktiv");
        inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository, hendelseTjenesteProvider);
        Mockito.clearInvocations(oidcRestClient);
    }

    @Test
    public void skal_kunne_konvertere_json_fra_person_feed_til_hendelse() throws IOException {
        // Last inn eksempel json og poll denne.
        String eksempelJson = lesPayloadFraFil("/eksempel-gyldig-person-feed-v2.json");
        Feed feed = JsonMapper.fromJson(eksempelJson, Feed.class);
        assertThat(feed).isNotNull();
        when(oidcRestClient.get(startUri, Feed.class)).thenReturn(feed);
        poller.poll(inputFeed);
        repoRule.getEntityManager().flush();

        // Sjekk at det har blitt opprettet hendelse for den ene fødselshendelsen som
        // ligger i json fil.
        List<InngåendeHendelse> inngåendeHendelser = hendelseRepository.finnHendelserSomErKlareTilGrovsortering();

        assertThat(inngåendeHendelser).hasSize(6); // Det er 1 FOEDSELSMELDINGOPPRETTET, 4 DOEDSMELDINGOPPRETTET og 1
                                                   // DOEDFOEDSELOPPRETTET i eksempel json.
        assertThat(inngåendeHendelser).flatExtracting(InngåendeHendelse::getSekvensnummer).contains(41L);// 41 er hentet
                                                                                                         // fra eksempel
                                                                                                         // json.
        assertThat(inngåendeHendelser).flatExtracting(InngåendeHendelse::getHåndtertStatus)
                .containsOnly(HåndtertStatusType.MOTTATT);
        List<HendelsePayload> hendelsePayloadListe = inngåendeHendelseTjeneste
                .getPayloadsForInngåendeHendelser(inngåendeHendelser);
        assertThat(hendelsePayloadListe).hasSize(6);

        FødselHendelsePayload fødselHendelsePayload = (FødselHendelsePayload) hendelsePayloadListe.stream()
                .filter(h -> h instanceof FødselHendelsePayload).findFirst().get();
        assertThat(fødselHendelsePayload.getAktørIdBarn()).isPresent();
        assertThat(fødselHendelsePayload.getAktørIdBarn().get()).contains("1678902101234");
        assertThat(fødselHendelsePayload.getAktørIdFar()).isPresent();
        assertThat(fødselHendelsePayload.getAktørIdFar().get()).contains("1929394959697");
        assertThat(fødselHendelsePayload.getAktørIdMor()).isPresent();
        assertThat(fødselHendelsePayload.getAktørIdMor().get()).contains("1572671828321");
        assertThat(fødselHendelsePayload.getFødselsdato()).isPresent();

        assertThat(hendelsePayloadListe.stream().filter(h -> h instanceof DødHendelsePayload)).hasSize(4);
        Set<String> dødHendelseAktørIder = hendelsePayloadListe.stream()
                .filter(h -> h instanceof DødHendelsePayload)
                .map(h -> ((DødHendelsePayload) h).getAktørId().get())
                .flatMap(java.util.Collection::stream)
                .collect(Collectors.toSet());
        assertThat(dødHendelseAktørIder)
                .containsAll(asList("1155334466777", "7890123456789", "1234565432123", "1357911131723"));

        DødfødselHendelsePayload dødfødselHendelsePayload = (DødfødselHendelsePayload) hendelsePayloadListe.stream()
                .filter(h -> h instanceof DødfødselHendelsePayload).findFirst().get();
        assertThat(dødfødselHendelsePayload.getAktørId()).isPresent();
        assertThat(dødfødselHendelsePayload.getAktørId().get()).contains("1235813213455");
        assertThat(dødfødselHendelsePayload.getDødfødselsdato()).isPresent();
    }

    private String lesPayloadFraFil(String filnavn) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(filnavn);
                Scanner scanner = new Scanner(resource, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        }
    }
}
