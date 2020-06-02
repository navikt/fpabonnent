package no.nav.foreldrepenger.abonnent.pdl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

public class TpsForsinkelseTjenesteTest {

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_onsdag_etter_06_30_gitt_opprettet_tid_mandag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 6, 13, 13);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 8, 6, 30),
                LocalDateTime.of(2020, 1, 8, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_torsdag_etter_06_30_gitt_opprettet_tid_tirsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 7, 9, 12);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 9, 6, 30),
                LocalDateTime.of(2020, 1, 9, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_fredag_etter_06_30_gitt_opprettet_tid_onsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 8, 22, 23);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 10, 6, 30),
                LocalDateTime.of(2020, 1, 10, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_mandag_etter_06_30_gitt_opprettet_tid_torsdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 9, 2, 49);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_fredag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 10, 19, 14);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_lørdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 11, 23, 59);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_hendelsen_kan_prosesseres_tirsdag_etter_06_30_gitt_opprettet_tid_søndag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 12, 1, 1);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSortering(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 14, 6, 30),
                LocalDateTime.of(2020, 1, 14, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_tirsdag_etter_06_30_gitt_sist_kjøring_på_mandag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 6, 6, 34);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 7, 6, 30),
                LocalDateTime.of(2020, 1, 7, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_fredag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 10, 6, 46);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

    @Test
    public void skal_utlede_at_neste_forsøk_på_prosessesering_er_mandag_etter_06_30_gitt_sist_kjøring_på_lørdag() {
        // Arrange
        LocalDateTime input = LocalDateTime.of(2020, 1, 11, 6, 30);

        // Act
        LocalDateTime resultat = new TpsForsinkelseTjeneste().finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(input);

        // Assert
        assertThat(resultat).isBetween(LocalDateTime.of(2020, 1, 13, 6, 30),
                LocalDateTime.of(2020, 1, 13, 6, 59));
    }

}