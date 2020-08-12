package no.nav.foreldrepenger.abonnent.feed.domain;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class AktørIdTjenesteTest {

    private static final String GYLDIG = "1000017373893";
    private static final String UGYLDIG = "x000017373893";

    @Test
    public void skal_bare_returnere_gyldige_aktørIder() {
        // Arrange
        HendelsePayload gyldig = new PdlFødselHendelsePayload.Builder().aktørIdForeldre(Set.of(GYLDIG)).build();
        HendelsePayload ugyldig = new PdlFødselHendelsePayload.Builder().aktørIdForeldre(Set.of(UGYLDIG)).build();

        List<HendelsePayload> hendelser = asList(gyldig, ugyldig);

        // Act
        List<String> aktørIderForSortering = AktørIdTjeneste.getAktørIderForSortering(hendelser);

        // Assert
        assertThat(aktørIderForSortering).containsOnly(GYLDIG);
    }
}