package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.extensions.CdiDbAwareTest;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;

@CdiDbAwareTest
public class HendelseTjenesteProviderTest {


    @Inject
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    @Test
    public void skal_finne_hendelsetjeneste() {
        // Act
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider.finnTjeneste(HendelseType.PDL_FØDSEL_OPPRETTET, "1");

        // Assert
        assertThat(hendelseTjeneste).isNotNull();
        assertThat(hendelseTjeneste).isInstanceOf(PdlFødselHendelseTjeneste.class);
    }

    @Test
    public void skal_kaste_feil_for_ukjent_hendelse() {

        // Act
        assertThrows(TekniskException.class, () -> hendelseTjenesteProvider.finnTjeneste(HendelseType.fraKode("-"), "1"));
    }
}