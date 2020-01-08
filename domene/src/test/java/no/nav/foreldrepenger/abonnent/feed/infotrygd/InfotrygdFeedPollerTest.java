package no.nav.foreldrepenger.abonnent.feed.infotrygd;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.InfotrygdAnnullert;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.InfotrygdEndret;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.InfotrygdInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class InfotrygdFeedPollerTest {

    private static final String BASE_URL_FEED = "https://infotrygd-hendelser-api-t4.nais.preprod.local/infotrygd/hendelser";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private HendelseRepository hendelseRepository;
    @Mock
    private OidcRestClient oidcRestClient;
    private InfotrygdFeedPoller poller;
    @Mock
    private InputFeed inputFeed;

    private URI startUri = URI.create(BASE_URL_FEED + "?sistLesteSekvensId=0&maxAntall=5");
    private URI endpoint = URI.create(BASE_URL_FEED);

    @Before
    public void setUp() {
        poller = new InfotrygdFeedPoller(endpoint, hendelseRepository, oidcRestClient, "5", true, 60);
        Mockito.clearInvocations(oidcRestClient);
    }

    @Test
    public void skal_lese_fra_feed() {
        // Arrange
        when(oidcRestClient.get(startUri, FeedDto.class)).thenReturn(lagTestData(false));

        // Act
        poller.poll(inputFeed);

        // Assert
        ArgumentCaptor<InngåendeHendelse> argumentCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(2)).lagreInngåendeHendelse(argumentCaptor.capture());
        verify(inputFeed).oppdaterLestOk("sistLesteSekvensId=3");
        InngåendeHendelse inngåendeHendelse = argumentCaptor.getValue();
        assertThat(inngåendeHendelse.getSekvensnummer()).isEqualTo(3);
        assertThat(inngåendeHendelse.getKoblingId()).isEqualTo(1);
        assertThat(inngåendeHendelse.getFeedKode()).isEqualTo(FeedKode.INFOTRYGD);
        assertThat(inngåendeHendelse.getType()).isEqualTo(HendelseType.INNVILGET);
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getHåndteresEtterTidspunkt()).isAfter(LocalDateTime.now());
    }

    @Test
    public void skal_lese_fra_feed_hvor_next_url_satt() {
        // Arrange
        when(inputFeed.getNextUrl()).thenReturn(Optional.of("sistLesteSekvensId=2"));

        // Act
        poller.poll(inputFeed);

        // Assert
        verify(oidcRestClient).get(URI.create(BASE_URL_FEED + "?sistLesteSekvensId=2&maxAntall=5"), FeedDto.class);
    }

    @Test
    public void skal_defaulte_til_base_url_hvis_next_url_invalid() {
        // Arrange
        when(inputFeed.getNextUrl()).thenReturn(Optional.of("foobar=2"));

        // Act
        poller.poll(inputFeed);

        // Assert
        verify(oidcRestClient).get(startUri, FeedDto.class);
    }

    @Test
    public void skal_ignorere_ukjent_items() {
        // Arrange
        FeedDto feedUtenAbonnerteItems = new FeedDto.Builder()
                .medTittel("enhetstest")
                .medElementer(singletonList(
                        lagEntry(2, new UkjentMelding(), 0)))
                .build();
        when(oidcRestClient.get(startUri, FeedDto.class)).thenReturn(feedUtenAbonnerteItems);

        // Act
        poller.poll(inputFeed);

        // Assert
        verify(hendelseRepository, times(0)).lagreInngåendeHendelse(any());
    }

    @Test
    public void skal_ignorere_endret_hendelser() {
        // Arrange
        when(oidcRestClient.get(startUri, FeedDto.class)).thenReturn(lagTestData(true));

        // Act
        poller.poll(inputFeed);

        // Assert
        ArgumentCaptor<InngåendeHendelse> argumentCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(2)).lagreInngåendeHendelse(argumentCaptor.capture());
        List<InngåendeHendelse> inngåendeHendelse = argumentCaptor.getAllValues();
        assertThat(inngåendeHendelse.stream()
                .filter(ih -> ih.getType().equals(Meldingstype.INFOTRYGD_ENDRET.getType()))
                .collect(Collectors.toList())).hasSize(0);
    }

    @Test
    public void skal_ikke_polle_feed_når_deaktivert() {
        // Arrange
        poller = new InfotrygdFeedPoller(endpoint, hendelseRepository, oidcRestClient, "5", false, 60);

        // Act
        poller.poll(inputFeed);

        // Assert
        verifyZeroInteractions(hendelseRepository);
        verifyZeroInteractions(oidcRestClient);
        verifyZeroInteractions(inputFeed);
    }

    private FeedDto lagTestData(boolean inkluderEndretHendelse) {
        List<FeedElement> elementer = new ArrayList<>();
        elementer.add(lagEntry(1, new InfotrygdAnnullert(), 0));
        elementer.add(lagEntry(2, new UkjentMelding(), 0));
        elementer.add(lagEntry(3, new InfotrygdInnvilget(), 1));
        if (inkluderEndretHendelse) {
            elementer.add(lagEntry(4, new InfotrygdEndret(), 0));
        }

        return new FeedDto.Builder()
                .medTittel("enhetstest")
                .medElementer(elementer)
                .build();
    }

    private FeedElement lagEntry(long sequence, Object melding, long koblingId) {
        String type;
        if (melding instanceof InfotrygdInnvilget){
            type = Meldingstype.INFOTRYGD_INNVILGET.getType();
        } else if (melding instanceof InfotrygdAnnullert){
            type = Meldingstype.INFOTRYGD_ANNULLERT.getType();
        } else {
            type = "Ukjent";
        }

        return new FeedElement.Builder()
            .medSekvensId(sequence)
            .medKoblingId(koblingId)
            .medType(type)
            .medInnhold(melding)
            .build();
    }

    private class UkjentMelding {

    }
}