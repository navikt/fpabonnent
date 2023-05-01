package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;

@ExtendWith(MockitoExtension.class)
class ForsinkelseTjenesteTest {

    @Mock
    private HendelseRepository hendelseRepository;
    @Mock
    private ForsinkelseKonfig forsinkelseKonfig;
    private ForsinkelseTjeneste forsinkelseTjeneste;

    private MockedStatic<DateUtil> mock;

    @BeforeEach
    void before() {
        mock = mockStatic(DateUtil.class);
        lenient().when(forsinkelseKonfig.skalForsinkeHendelser()).thenReturn(true);
        lenient().when(forsinkelseKonfig.normalForsinkelseMinutter()).thenReturn(60);
        forsinkelseTjeneste = new ForsinkelseTjeneste(forsinkelseKonfig, hendelseRepository);
    }

    @AfterEach
    void after() {
        mock.close();
    }

    private void settTid(LocalDateTime tid) {
        mock.when(DateUtil::now).thenReturn(tid);
    }

    @Test
    void skal_utlede_at_hendelsen_kan_prosesseres_10_30_gitt_at_klokka_er_09_30() {
        // Arrange
        settTid(LocalDateTime.of(2020, 1, 6, 9, 30));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isEqualTo(LocalDateTime.of(2020, 1, 6, 10, 30));
    }

    @Test
    void skal_utlede_at_hendelsen_kan_prosesseres_etter_06_30_gitt_at_klokka_er_01_30() {
        // Arrange
        settTid(LocalDateTime.of(2020, 1, 6, 1, 30));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 6, 6, 30), LocalDateTime.of(2020, 1, 6, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelsen_kan_prosesseres_torsdag_etter_06_30_gitt_at_klokka_er_23_45_på_onsdag() {
        // Arrange
        settTid(LocalDateTime.of(2020, 1, 8, 23, 45));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 9, 6, 30), LocalDateTime.of(2020, 1, 9, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelsen_kan_prosesseres_mandag_etter_06_30_gitt_at_det_er_lørdag() {
        // Arrange
        settTid(LocalDateTime.of(2020, 1, 11, 2, 49));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30), LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelsen_kan_prosesseres_mandag_etter_06_30_gitt_at_det_er_søndag() {
        // Arrange
        settTid(LocalDateTime.of(2020, 1, 12, 1, 1));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30), LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelse_mottatt_torsdag_etter_stengetid_kan_prosesseres_mandag_etter_06_30_pga_17_mai_fredag() {
        // Arrange
        settTid(LocalDateTime.of(2019, 5, 16, 23, 55));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2019, 5, 20, 6, 30), LocalDateTime.of(2019, 5, 20, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelse_mottatt_mandag_17_mai_kan_prosesseres_tirsdag_etter_06_30() {
        // Arrange
        settTid(LocalDateTime.of(2027, 5, 17, 13, 6));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2027, 5, 18, 6, 30), LocalDateTime.of(2027, 5, 18, 6, 59));
    }

    @Test
    void skal_utlede_at_hendelse_mottatt_fredag_etter_stengetid_kan_prosesseres_onsdag_etter_06_30_pga_juledager_mandag_og_tirsdag() {
        // Arrange
        settTid(LocalDateTime.of(2017, 12, 22, 23, 34));

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2017, 12, 27, 6, 30), LocalDateTime.of(2017, 12, 27, 6, 59));
    }

    @Test
    void skal_utlede_at_neste_forsøk_på_prosessesering_er_tirsdag_etter_06_30_gitt_sist_kjøring_på_mandag() {
        // Arrange
        var input = LocalDateTime.of(2020, 1, 6, 6, 34);
        settTid(input);

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 7, 6, 30), LocalDateTime.of(2020, 1, 7, 6, 59));
    }

    @Test
    void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_fredag() {
        // Arrange
        var input = LocalDateTime.of(2020, 1, 10, 6, 46);
        settTid(input);

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30), LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_lørdag() {
        // Arrange
        var input = LocalDateTime.of(2020, 1, 11, 6, 30);
        settTid(input);

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30), LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_torsdag_og_fredag_er_første_januar() {
        // Arrange
        var input = LocalDateTime.of(2020, 12, 31, 6, 34);
        settTid(input);

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2021, 1, 4, 6, 30), LocalDateTime.of(2021, 1, 4, 6, 59));
    }

    @Test
    void skal_utlede_at_neste_forsøk_på_prosessesering_er_tirsdag_etter_06_30_gitt_sist_kjøring_på_fredag_og_mandag_er_første_mai() {
        // Arrange
        var input = LocalDateTime.of(2023, 4, 28, 6, 43);
        settTid(input);

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input, mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2023, 5, 2, 6, 30), LocalDateTime.of(2023, 5, 2, 6, 59));
    }

    @Test
    void skal_utlede_at_en_hendelse_som_følger_en_tidligere_uhåndtert_hendelse_skal_behandles_2_minutter_etter() {
        // Arrange
        var tidspunktA = LocalDateTime.of(2023, 4, 17, 13, 6);
        settTid(tidspunktA);

        var hendelseA = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .håndteresEtterTidspunkt(tidspunktA)
            .build();
        var hendelseB = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .tidligereHendelseId("A")
            .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes("A", HendelseKilde.PDL)).thenReturn(Optional.of(hendelseA));

        // Act
        var resultat1 = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(hendelseB);
        var resultat2 = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), hendelseB);

        // Assert
        assertThat(resultat1).isEqualTo(tidspunktA.plusMinutes(2));
        assertThat(resultat2).isEqualTo(tidspunktA.plusMinutes(2));
    }

    @Test
    void skal_utlede_at_en_hendelse_som_følger_en_tidligere_uhåndtert_hendelse_som_skulle_vært_håndtert_tilbake_i_tid_skal_behandles_etter_retry_all() {
        // Arrange
        var tidspunktA = LocalDateTime.of(2023, 4, 25, 13, 6);
        settTid(tidspunktA.plusDays(2));

        var hendelseA = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .håndteresEtterTidspunkt(tidspunktA)
            .build();
        var hendelseB = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .tidligereHendelseId("A")
            .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes("A", HendelseKilde.PDL)).thenReturn(Optional.of(hendelseA));

        // Act
        var resultat1 = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(hendelseB);
        var resultat2 = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), hendelseB);

        // Assert
        var nesteDagEtterRetryAll = DateUtil.now().plusDays(1).withHour(7).withMinute(30).withSecond(0).withNano(0);
        assertThat(resultat1).isEqualTo(nesteDagEtterRetryAll);
        assertThat(resultat2).isEqualTo(nesteDagEtterRetryAll);
    }

    @Test
    void skal_utlede_at_en_hendelse_som_følger_en_tidligere_håndtert_hendelse_skal_behandles_uavhengig() {
        // Arrange
        var tidspunktA = LocalDateTime.now().plusDays(2);
        var innsendingstidspunkt = LocalDateTime.of(2023, 4, 28, 6, 43);
        settTid(innsendingstidspunkt);

        var hendelseA = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .håndteresEtterTidspunkt(tidspunktA)
            .build();
        var hendelseB = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .tidligereHendelseId("A")
            .build();

        when(hendelseRepository.finnHendelseFraIdHvisFinnes("A", HendelseKilde.PDL)).thenReturn(Optional.of(hendelseA));

        // Act
        var resultat1 = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(hendelseB);
        var resultat2 = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(innsendingstidspunkt, hendelseB);

        // Assert
        assertThat(resultat1).isEqualTo(innsendingstidspunkt.plusHours(1));
        assertThat(resultat2).isBetween(LocalDateTime.of(2023, 5, 2, 6, 30), LocalDateTime.of(2023, 5, 2, 6, 59));
    }

    @Test
    void skal_ikke_forsinke_hendelser_når_konfig_er_deaktivert() {
        // Arrange
        when(forsinkelseKonfig.skalForsinkeHendelser()).thenReturn(false);
        settTid(LocalDateTime.now());

        // Act
        var resultat = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(mock(InngåendeHendelse.class));

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
    }
}
