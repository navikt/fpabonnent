package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.Person;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

class ForeldreTjenesteTest {
    private static final PersonIdent BARN_FNR = PersonIdent.randomBarn();
    private static final PersonIdent MOR_FNR = PersonIdent.randomMor();
    private static final PersonIdent FAR_FNR = PersonIdent.randomFar();

    private FødselTjeneste fødselTjeneste;

    private Persondata pdlMock = mock(Persondata.class);

    @BeforeEach
    void setup() {
        fødselTjeneste = new FødselTjeneste(pdlMock);
    }

    @Test
    void skal_hente_foreldre_for_fnr() {
        // Arrange
        Person person = new Person();
        ForelderBarnRelasjon familierelasjonMor = new ForelderBarnRelasjon(MOR_FNR.getIdent(), ForelderBarnRelasjonRolle.MOR,
            ForelderBarnRelasjonRolle.BARN, null, null, null);
        ForelderBarnRelasjon familierelasjonFar = new ForelderBarnRelasjon(FAR_FNR.getIdent(), ForelderBarnRelasjonRolle.FAR,
            ForelderBarnRelasjonRolle.BARN, null, null, null);
        person.setForelderBarnRelasjon(List.of(familierelasjonMor, familierelasjonFar));
        when(pdlMock.hentPerson(any(), any())).thenReturn(person);

        // Act
        List<PersonIdent> foreldre = fødselTjeneste.hentForeldreTil(BARN_FNR);

        // Assert
        assertThat(foreldre.stream().map(PersonIdent::getIdent).collect(Collectors.toSet())).containsAll(
            Set.of(MOR_FNR.getIdent(), FAR_FNR.getIdent()));
    }
}
