package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;

class FødselTjenesteTest {

    private static final PersonIdent BARN_FNR = PersonIdent.randomBarn();
    private static final PersonIdent MOR_FNR = PersonIdent.randomMor();
    private static final AktørId MOR_AKTØR_ID = new AktørId("1111111111111");
    private static final PersonIdent FAR_FNR = PersonIdent.randomFar();
    private static final AktørId FAR_AKTØR_ID = new AktørId("2222222222222");

    private FødselTjeneste fødselTjeneste = mock(FødselTjeneste.class);
    private AktørTjeneste aktørTjeneste = mock(AktørTjeneste.class);
    private ForeldreTjeneste foreldreTjeneste;

    @BeforeEach
    void setup() {
        foreldreTjeneste = new ForeldreTjeneste(fødselTjeneste, aktørTjeneste);
    }

    @Test
    void skal_hente_foreldre_for_fnr() {
        // Arrange
        when(fødselTjeneste.hentForeldreTil(BARN_FNR)).thenReturn(List.of(MOR_FNR, FAR_FNR));
        when(aktørTjeneste.hentAktørIdForPersonIdent(MOR_FNR)).thenReturn(Optional.of(MOR_AKTØR_ID));
        when(aktørTjeneste.hentAktørIdForPersonIdent(FAR_FNR)).thenReturn(Optional.of(FAR_AKTØR_ID));

        // Act
        Set<AktørId> foreldre = foreldreTjeneste.hentForeldre(BARN_FNR);

        // Assert
        assertThat(foreldre.stream()).containsAll(Set.of(MOR_AKTØR_ID, FAR_AKTØR_ID));
    }
}
