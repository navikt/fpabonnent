package no.nav.foreldrepenger.abonnent.feed.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.feed.tps.TpsFeedPoller;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.tjenester.person.feed.common.v1.Feed;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.Meldingstype;
import no.nav.tjenester.person.feed.v2.foedselsmelding.FoedselsmeldingOpprettet;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class TpsFeedPollerTest {

    private static final String BASE_URL_FEED = "http://foo.bar/tpsfeed";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private HendelseRepository hendelseRepository;
    @Mock
    private OidcRestClient oidcRestClient;
    private TpsFeedPoller poller;
    @Mock
    private InputFeed inputFeed;

    private URI startUri = URI.create(BASE_URL_FEED + "?sequenceId=1&pageSize=5");
    private URI endpoint = URI.create(BASE_URL_FEED);

    @Before
    public void setUp() {
        poller = new TpsFeedPoller(endpoint, hendelseRepository, oidcRestClient, "5", true);
        Mockito.clearInvocations(oidcRestClient);
    }

    @Test
    public void skal_lese_fra_feed() throws Exception {
        when(oidcRestClient.get(startUri, Feed.class)).thenReturn(lagTestData());
        poller.poll(inputFeed);
        verify(hendelseRepository, times(2)).lagreInngåendeHendelse(any());
        verify(inputFeed).oppdaterLestOk("sequenceId=4");
    }

    @Test
    public void skal_lese_fra_feed_hvor_next_url_satt() throws Exception {
        when(inputFeed.getNextUrl()).thenReturn(Optional.of("sequenceId=2"));
        poller.poll(inputFeed);

        verify(oidcRestClient).get(URI.create(BASE_URL_FEED + "?sequenceId=2&pageSize=5"), Feed.class);
    }

    @Test
    public void skal_defaulte_til_base_url_hvis_next_url_invalid() throws Exception {
        when(inputFeed.getNextUrl()).thenReturn(Optional.of("foobar=2"));
        poller.poll(inputFeed);

        verify(oidcRestClient).get(startUri, Feed.class);
    }

    @Test
    public void skal_kjøre_ok_uten_items_i_feed() {
        when(oidcRestClient.get(startUri, Feed.class)).thenReturn(Feed.builder().build());
        poller.poll(inputFeed);
        verify(hendelseRepository, times(0)).lagreInngåendeHendelse(any());
    }

    @Test
    public void skal_ignorere_ukjent_items() {
        Feed feedUtenAbonnerteItems = Feed.builder()
                .title("enhetstest")
                .items(Arrays.asList(
                        lagEntry(2, new UkjentMelding())))
                .build();
        when(oidcRestClient.get(startUri, Feed.class)).thenReturn(feedUtenAbonnerteItems);

        poller.poll(inputFeed);
        verify(hendelseRepository, times(0)).lagreInngåendeHendelse(any());
    }

    @Test
    public void skal_ignorere_hvis_jsonfeed_returnerer_null() {
        poller.poll(inputFeed);
        verify(oidcRestClient).get(startUri, Feed.class);
        verify(inputFeed).oppdaterFeilet();
    }

    @Test
    public void skal_ikke_polle_feed_når_deaktivert() {
        // Arrange
        poller = new TpsFeedPoller(endpoint, hendelseRepository, oidcRestClient, "5", false);

        // Act
        poller.poll(inputFeed);

        // Assert
        verifyZeroInteractions(hendelseRepository);
        verifyZeroInteractions(oidcRestClient);
        verifyZeroInteractions(inputFeed);
    }

    @Test
    public void skal_kunne_konvertere_json_fra_person_feed_til_hendelse() throws IOException {
        String eksempelJson = lesPayloadFraFil("/eksempel-gyldig-person-feed-v2.json");
        Feed feed = JsonMapper.fromJson(eksempelJson, Feed.class);
        assertThat(feed).isNotNull();
        when(oidcRestClient.get(startUri, Feed.class)).thenReturn(feed);
        poller.poll(inputFeed);
        // Eksempel json inneholder 6 meldinger med type som er støttet:
        verify(hendelseRepository, times(6)).lagreInngåendeHendelse(any());
    }

    private String lesPayloadFraFil(String filnavn) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(filnavn);
                Scanner scanner = new Scanner(resource, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        }
    }

    private Feed lagTestData() {
        return Feed.builder()
                .title("enhetstest")
                .items(Arrays.asList(
                        lagEntry(1, new FoedselsmeldingOpprettet()),
                        lagEntry(2, new UkjentMelding()),
                        lagEntry(3, new FoedselsmeldingOpprettet())))
                .build();
    }

    private FeedEntry lagEntry(long sequence, Object melding) {
        String type;
        if (melding instanceof UkjentMelding) {
            type = "Ukjent";
        } else {
            type = Meldingstype.FOEDSELSMELDINGOPPRETTET.name();
        }

        return FeedEntry.builder()
                .sequence(sequence)
                .type(type)
                .content(melding)
                .build();
    }

    private class UkjentMelding {

    }
}
