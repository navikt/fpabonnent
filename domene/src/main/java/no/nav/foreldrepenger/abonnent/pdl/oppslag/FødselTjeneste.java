package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.pdl.Familierelasjon;
import no.nav.pdl.FamilierelasjonResponseProjection;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.Pdl;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@ApplicationScoped
public class FødselTjeneste {

    private Pdl pdlKlient;

    FødselTjeneste() {
        // CDI
    }

    @Inject
    public FødselTjeneste(@Jersey Pdl pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public List<PersonIdent> hentForeldreTil(PersonIdent barn) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.getIdent());
        var projection = new PersonResponseProjection()
                .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());

        var person = pdlKlient.hentPerson(request, projection);

        return person.getFamilierelasjoner().stream()
                .filter(f -> !Familierelasjonsrolle.BARN.equals(f.getRelatertPersonsRolle()))
                .map(Familierelasjon::getRelatertPersonsIdent)
                .map(PersonIdent::fra)
                .collect(Collectors.toList());
    }
}
