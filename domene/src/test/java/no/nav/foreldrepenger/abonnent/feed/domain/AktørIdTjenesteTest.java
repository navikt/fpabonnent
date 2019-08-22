package no.nav.foreldrepenger.abonnent.feed.domain;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class AktørIdTjenesteTest {

    private static final String GYLDIG = "1000017373893";
    private static final String UGYLDIG = "x000017373893";

    @Test
    public void skal_bare_returnere_gyldige_aktørIder() {
        // Arrange
        HendelsePayload gyldig = new InfotrygdHendelsePayload.Builder().aktørId(GYLDIG).build();
        HendelsePayload ugyldig = new InfotrygdHendelsePayload.Builder().aktørId(UGYLDIG).build();

        List<HendelsePayload> hendelser = asList(gyldig, ugyldig);

        // Act
        List<String> aktørIderForSortering = AktørIdTjeneste.getAktørIderForSortering(hendelser);

        // Assert
        assertThat(aktørIderForSortering).containsOnly(GYLDIG);
    }
}