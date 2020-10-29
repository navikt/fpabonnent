package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;

public class TpsForsinkelseTjenesteTest {

    private HendelseRepository hendelseRepository = mock(HendelseRepository.class);

    private TpsForsinkelseKonfig tpsForsinkelseKonfig;
    private TpsForsinkelseTjeneste tpsForsinkelseTjeneste;

    @Before
    public void before() {
        tpsForsinkelseKonfig = mock(TpsForsinkelseKonfig.class);
        when(tpsForsinkelseKonfig.skalForsinkeHendelser()).thenReturn(true);
        tpsForsinkelseTjeneste = new TpsForsinkelseTjeneste(tpsForsinkelseKonfig, hendelseRepository);
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_onsdag_etter_06_30_gitt_opprettet_tid_mandag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 6, 13, 13);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 8, 6, 30),
                LocalDateTime.of(2020, 1, 8, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_torsdag_etter_06_30_gitt_opprettet_tid_tirsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 7, 9, 12);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 9, 6, 30),
                LocalDateTime.of(2020, 1, 9, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_fredag_etter_06_30_gitt_opprettet_tid_onsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 8, 22, 23);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 10, 6, 30),
                LocalDateTime.of(2020, 1, 10, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_mandag_etter_06_30_gitt_opprettet_tid_torsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 9, 2, 49);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_fredag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 10, 19, 14);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_lørdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 11, 23, 59);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_søndag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 12, 1, 1);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelse_fra_onsdag_kan_prosesseres_mandag_etter_06_30_pga_17_mai_fredag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2019, 5, 15, 14, 2);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2019, 5, 20, 6, 30),
                LocalDateTime.of(2019, 5, 20, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelse_fra_torsdag_kan_prosesseres_tirsdag_etter_06_30_pga_17_mai_mandag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2021, 5, 13, 13, 6);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2021, 5, 18, 6, 30),
                LocalDateTime.of(2021, 5, 18, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelse_fra_fredag_kan_prosesseres_torsdag_etter_06_30_pga_juledager_tirsdag_og_onsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2018, 12, 21, 9, 3);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2018, 12, 27, 6, 30),
                LocalDateTime.of(2018, 12, 27, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_tirsdag_etter_06_30_gitt_sist_kjøring_på_mandag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 6, 6, 34);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 7, 6, 30),
                LocalDateTime.of(2020, 1, 7, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_fredag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 10, 6, 46);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_lørdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 11, 6, 30);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_torsdag_og_fredag_er_første_januar() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 12, 31, 6, 34);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2021, 1, 4, 6, 30),
                LocalDateTime.of(2021, 1, 4, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_tirsdag_etter_06_30_gitt_sist_kjøring_på_fredag_og_mandag_er_første_mai() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2023, 4, 28, 6, 43);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2023, 5, 2, 6, 30),
                LocalDateTime.of(2023, 5, 2, 6, 59));
    }

    @Test
    public void skal_utlede_at_en_hendelse_som_følger_en_tidligere_uhåndtert_hendelse_skal_behandles_2_minutter_etter() {
        // Arrange
        LocalDateTime tidspunktA = LocalDateTime.now().plusDays(2);

        InngåendeHendelse hendelseA = InngåendeHendelse.builder()
                .hendelseId("A")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(tidspunktA)
                .build();
        InngåendeHendelse hendelseB = InngåendeHendelse.builder()
                .hendelseId("B")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .tidligereHendelseId("A")
                .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes(eq("A"), eq(HendelseKilde.PDL))).thenReturn(Optional.of(hendelseA));

        // Act
        LocalDateTime resultat1 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(LocalDateTime.now(), hendelseB);
        LocalDateTime resultat2 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), hendelseB);

        // Assert
        assertThat(resultat1).isEqualTo(tidspunktA.plusMinutes(2));
        assertThat(resultat2).isEqualTo(tidspunktA.plusMinutes(2));
    }

    @Test
    public void skal_utlede_at_en_hendelse_som_følger_en_tidligere_uhåndtert_hendelse_som_skulle_vært_håndtert_tilbake_i_tid_skal_behandles_etter_retry_all() {
        // Arrange
        LocalDateTime tidspunktA = LocalDateTime.now().minusDays(2);

        InngåendeHendelse hendelseA = InngåendeHendelse.builder()
                .hendelseId("A")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(tidspunktA)
                .build();
        InngåendeHendelse hendelseB = InngåendeHendelse.builder()
                .hendelseId("B")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .tidligereHendelseId("A")
                .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes(eq("A"), eq(HendelseKilde.PDL))).thenReturn(Optional.of(hendelseA));

        // Act
        LocalDateTime resultat1 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(LocalDateTime.now(), hendelseB);
        LocalDateTime resultat2 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), hendelseB);

        // Assert
        LocalDateTime nesteDagEtterRetryAll = LocalDateTime.now().plusDays(1).withHour(7).withMinute(30).withSecond(0).withNano(0);
        assertThat(resultat1).isEqualTo(nesteDagEtterRetryAll);
        assertThat(resultat2).isEqualTo(nesteDagEtterRetryAll);
    }

    @Test
    public void skal_utlede_at_en_hendelse_som_følger_en_tidligere_håndtert_hendelse_skal_behandles_uavhengig() {
        // Arrange
        LocalDateTime tidspunktA = LocalDateTime.now().plusDays(2);
        LocalDateTime input = LocalDateTime.of(2023, 4, 28, 6, 43);

        InngåendeHendelse hendelseA = InngåendeHendelse.builder()
                .hendelseId("A")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .håndteresEtterTidspunkt(tidspunktA)
                .build();
        InngåendeHendelse hendelseB = InngåendeHendelse.builder()
                .hendelseId("B")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .tidligereHendelseId("A")
                .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes(eq("A"), eq(HendelseKilde.PDL))).thenReturn(Optional.of(hendelseA));

        // Act
        LocalDateTime resultat1 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(input, hendelseB);
        LocalDateTime resultat2 = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, hendelseB);

        // Assert
        assertThat(resultat1).isBetween(LocalDateTime.of(2023, 5, 2, 6, 30),
                LocalDateTime.of(2023, 5, 2, 6, 59));
        assertThat(resultat2).isBetween(LocalDateTime.of(2023, 5, 2, 6, 30),
                LocalDateTime.of(2023, 5, 2, 6, 59));
    }

    @Test
    public void skal_ikke_forsinke_hendelser_når_konfig_er_deaktivert() {
        // Arrange
        when(tpsForsinkelseKonfig.skalForsinkeHendelser()).thenReturn(false);

        // Act
        LocalDateTime resultat = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(LocalDateTime.now(), mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
    }
}