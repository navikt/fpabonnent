package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AktørIdTjenesteTest {

    private static final String GYLDIG = "1000017373893";
    private static final String UGYLDIG = "x000017373893";

    @Test
    void skal_bare_returnere_gyldige_aktørIder() {
        // Arrange
        HendelsePayload hendelse = new PdlFødselHendelsePayload.Builder().aktørIdForeldre(Set.of(GYLDIG, UGYLDIG)).build();

        // Act
        var aktørIderForSortering = AktørIdTjeneste.getAktørIderForSortering(hendelse);

        // Assert
        assertThat(aktørIderForSortering).containsOnly(GYLDIG);
    }
}
