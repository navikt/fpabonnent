package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class HendelseTjenesteProviderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        // Assert
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-309345");

        // Act
        hendelseTjenesteProvider.finnTjeneste(HendelseType.fraKode("-"), "1");
    }
}