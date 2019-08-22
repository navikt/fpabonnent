package no.nav.foreldrepenger.abonnent.felles;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.tps.FødselsmeldingOpprettetHendelseTjeneste;
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
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider.finnTjeneste(HendelseType.FØDSELSMELDINGOPPRETTET, 1L);

        // Assert
        assertThat(hendelseTjeneste).isNotNull();
        assertThat(hendelseTjeneste).isInstanceOf(FødselsmeldingOpprettetHendelseTjeneste.class);
    }

    @Test
    public void skal_kaste_feil_for_ukjent_hendelse() {
        // Assert
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-309345");

        // Act
        hendelseTjenesteProvider.finnTjeneste(new HendelseType("ukjent_type"), 1L);
    }
}