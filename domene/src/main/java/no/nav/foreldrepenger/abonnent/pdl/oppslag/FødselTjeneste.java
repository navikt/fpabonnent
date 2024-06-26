package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

@ApplicationScoped
public class FødselTjeneste {

    private Persondata pdlKlient;

    FødselTjeneste() {
        // CDI
    }

    @Inject
    public FødselTjeneste(Persondata pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public List<PersonIdent> hentForeldreTil(PersonIdent barn) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.getIdent());
        var projection = new PersonResponseProjection().forelderBarnRelasjon(
            new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());

        var person = pdlKlient.hentPerson(request, projection);

        return person.getForelderBarnRelasjon()
            .stream()
            .filter(f -> !ForelderBarnRelasjonRolle.BARN.equals(f.getRelatertPersonsRolle()))
            .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
            .filter(Objects::nonNull)
            .map(PersonIdent::fra)
            .toList();
    }

    public Optional<LocalDate> hentFødselsdato(PersonIdent barn) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.getIdent());
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPerson(request, projection);

        return person.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst()
            .map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
