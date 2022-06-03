package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.RelatertBiPersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.Pdl;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@ApplicationScoped
public class FødselTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FødselTjeneste.class);

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
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle()
                    .relatertPersonUtenFolkeregisteridentifikator(new RelatertBiPersonResponseProjection().kjoenn().foedselsdato()));

        var person = pdlKlient.hentPerson(request, projection);

        var foreldreUtenIdent = person.getForelderBarnRelasjon().stream()
            .filter(f -> !ForelderBarnRelasjonRolle.BARN.equals(f.getRelatertPersonsRolle()))
            .filter(f -> f.getRelatertPersonsIdent() == null)
            .map(ForelderBarnRelasjon::getRelatertPersonUtenFolkeregisteridentifikator)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!foreldreUtenIdent.isEmpty()) {
            LOG.warn("Forelder uten ident ignoreres {}", foreldreUtenIdent);
        }

        return person.getForelderBarnRelasjon().stream()
                .filter(f -> !ForelderBarnRelasjonRolle.BARN.equals(f.getRelatertPersonsRolle()))
                .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
                .filter(Objects::nonNull)
                .map(PersonIdent::fra)
                .collect(Collectors.toList());
    }
}
