package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.FolkeregistermetadataResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonBostedsadresseParametrizedInput;
import no.nav.pdl.PersonFolkeregisterpersonstatusParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.Pdl;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class UtflyttingsDatoTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtflyttingsDatoTjeneste.class);

    private Pdl pdlKlient;

    UtflyttingsDatoTjeneste() {
        // CDI
    }

    @Inject
    public UtflyttingsDatoTjeneste(@Jersey Pdl pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public LocalDate finnUtflyttingsdato(String aktørId, String hendelseId) {
        var idag = LocalDate.now();
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId);

        var projection = new PersonResponseProjection()
                .folkeregisterpersonstatus(new PersonFolkeregisterpersonstatusParametrizedInput().historikk(true),
                        new FolkeregisterpersonstatusResponseProjection().status()
                                .folkeregistermetadata(new FolkeregistermetadataResponseProjection().ajourholdstidspunkt().gyldighetstidspunkt()))
                .bostedsadresse(new PersonBostedsadresseParametrizedInput().historikk(true),
                        new BostedsadresseResponseProjection().gyldigFraOgMed().gyldigTilOgMed());

        var person = pdlKlient.hentPerson(query, projection);

        var fraPersonStatus = person.getFolkeregisterpersonstatus().stream()
                .filter(f -> "utflyttet".equals(f.getStatus()))
                .map(UtflyttingsDatoTjeneste::personstatusGyldigFra)
                .filter(d -> d != null && d.isAfter(idag.minusMonths(6)))
                .max(Comparator.naturalOrder());

        var fraBostedsAdresse = person.getBostedsadresse().stream()
                .max(Comparator.comparing(UtflyttingsDatoTjeneste::bostedsAdresseFraDato)
                        .thenComparing(UtflyttingsDatoTjeneste::bostedsAdresseTilDato))
                .map(Bostedsadresse::getGyldigTilOgMed)
                .map(UtflyttingsDatoTjeneste::localDateFraDate)
                .filter(d -> d.isAfter(idag.minusMonths(6)));

        LOGGER.info("Utflyttingshendelse {} fant datoer: personstatus {} bosted {}", hendelseId, fraPersonStatus, fraBostedsAdresse);

        if (fraPersonStatus.isEmpty() && fraBostedsAdresse.isEmpty()) {
            return idag;
        } else if (fraPersonStatus.isPresent() && fraBostedsAdresse.isPresent()) {
            return fraBostedsAdresse.get().isBefore(fraPersonStatus.get()) ? fraBostedsAdresse.get() : fraPersonStatus.get();
        } else {
            return fraPersonStatus.orElseGet(fraBostedsAdresse::get);
        }
    }

    private static LocalDate personstatusGyldigFra(Folkeregisterpersonstatus status) {
        var ajourFom = status.getFolkeregistermetadata().getAjourholdstidspunkt(); // TODO evaluer
        var gyldigFom = status.getFolkeregistermetadata().getGyldighetstidspunkt();
        Date brukFom;
        if (ajourFom != null && gyldigFom != null) {
            brukFom = ajourFom.before(gyldigFom) ? ajourFom : gyldigFom;
        } else {
            brukFom = gyldigFom != null ? gyldigFom : ajourFom;
        }
        return localDateFraDate(brukFom);
    }

    private static LocalDate bostedsAdresseFraDato(Bostedsadresse bostedsadresse) {
        return bostedsadresse.getGyldigFraOgMed() == null ? Tid.TIDENES_BEGYNNELSE : localDateFraDate(bostedsadresse.getGyldigFraOgMed());
    }

    private static LocalDate bostedsAdresseTilDato(Bostedsadresse bostedsadresse) {
        return bostedsadresse.getGyldigTilOgMed() == null ? Tid.TIDENES_ENDE  : localDateFraDate(bostedsadresse.getGyldigTilOgMed());
    }

    private static LocalDate localDateFraDate(Date fom) {
        return fom == null ? null : LocalDateTime.ofInstant(fom.toInstant(), ZoneId.systemDefault()).toLocalDate();
    }
}
