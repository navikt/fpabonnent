package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.foreldrepenger.abonnent.testutilities.FiktiveFnr;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Identliste;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

class AktørTjenesteTest {
    private static final PersonIdent FNR = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static final AktørId AKTØR_ID = AktørId.dummy();

    private AktørTjeneste aktørTjeneste;

    private Persondata pdlMock = mock(Persondata.class);

    @BeforeEach
    void setup() {
        aktørTjeneste = new AktørTjeneste(pdlMock);
    }

    @Test
    void skal_hente_aktør_for_fnr() {
        // Arrange
        when(pdlMock.hentIdenter(any(), any())).thenReturn(
            new Identliste(List.of(new IdentInformasjon(AKTØR_ID.getId(), IdentGruppe.AKTORID, false))));

        // Act
        Optional<AktørId> optAktørId = aktørTjeneste.hentAktørIdForPersonIdent(FNR);

        // Assert
        assertThat(optAktørId).isPresent().hasValueSatisfying(v -> assertThat(v).isEqualTo(AKTØR_ID));
    }
}
