package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

class HendelseTjenesteProviderTest {


    private HendelseTjenesteProvider hendelseTjenesteProvider;

    @Test
    void skal_finne_hendelsetjeneste() {
        // Act
        hendelseTjenesteProvider = new HendelseTjenesteProvider(new UnitTestLookupInstanceImpl<>(new PdlFødselHendelseTjeneste()));
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider.finnTjeneste(HendelseType.PDL_FØDSEL_OPPRETTET, "1");

        // Assert
        assertThat(hendelseTjeneste).isNotNull().isInstanceOf(PdlFødselHendelseTjeneste.class);
    }

}
