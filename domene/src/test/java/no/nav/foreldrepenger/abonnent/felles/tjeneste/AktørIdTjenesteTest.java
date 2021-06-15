package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;

public class AktørIdTjenesteTest {

    private static final String GYLDIG = "1000017373893";
    private static final String UGYLDIG = "x000017373893";

    @Test
    public void skal_bare_returnere_gyldige_aktørIder() {
        HendelsePayload hendelse = new PdlFødselHendelsePayload.Builder().aktørIdForeldre(Set.of(GYLDIG, UGYLDIG)).build();

        List<String> aktørIderForSortering = AktørIdTjeneste.getAktørIderForSortering(hendelse);

        assertThat(aktørIderForSortering).containsOnly(GYLDIG);
    }
}