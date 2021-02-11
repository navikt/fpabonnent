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
import no.nav.foreldrepenger.abonnent.testutilities.FiktiveFnr;
import no.nav.pdl.Familierelasjon;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.Person;
import no.nav.vedtak.felles.integrasjon.pdl.Pdl;

public class ForeldreTjenesteTest {
    private static final PersonIdent BARN_FNR = new PersonIdent(new FiktiveFnr().nesteBarnFnr());
    private static final PersonIdent MOR_FNR = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static final PersonIdent FAR_FNR = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());

    private FødselTjeneste fødselTjeneste;

    private Pdl pdlMock = mock(Pdl.class);

    @BeforeEach
    public void setup() {
        fødselTjeneste = new FødselTjeneste(pdlMock);
    }

    @Test
    public void skal_hente_foreldre_for_fnr() {
        // Arrange
        Person person = new Person();
        Familierelasjon familierelasjonMor = new Familierelasjon(MOR_FNR.getIdent(), Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN, null, null);
        Familierelasjon familierelasjonFar = new Familierelasjon(FAR_FNR.getIdent(), Familierelasjonsrolle.FAR, Familierelasjonsrolle.BARN, null, null);
        person.setFamilierelasjoner(List.of(familierelasjonMor, familierelasjonFar));
        when(pdlMock.hentPerson(any(), any())).thenReturn(person);

        // Act
        List<PersonIdent> foreldre = fødselTjeneste.hentForeldreTil(BARN_FNR);

        // Assert
        assertThat(foreldre.stream().map(PersonIdent::getIdent).collect(Collectors.toSet())).containsAll(Set.of(MOR_FNR.getIdent(), FAR_FNR.getIdent()));
    }
}