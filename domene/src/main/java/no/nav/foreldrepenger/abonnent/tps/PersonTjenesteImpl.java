package no.nav.foreldrepenger.abonnent.tps;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

@ApplicationScoped
public class PersonTjenesteImpl implements PersonTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonTjenesteImpl.class);

    private AktørConsumerMedCache aktørConsumer;
    private PersonConsumer personConsumer;

    public PersonTjenesteImpl() {
    }

    @Inject
    public PersonTjenesteImpl(AktørConsumerMedCache aktørConsumer,
                              PersonConsumer personConsumer) {
        this.aktørConsumer = aktørConsumer;
        this.personConsumer = personConsumer;
    }

    @Override
    public Set<AktørId> registrerteForeldre(AktørId aktørId) {
        PersonIdent personIdent;
        try {
            personIdent = hentIdentForAktørId(aktørId);
        } catch (TekniskException e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            LOGGER.info("Feilet ved kall til aktør-registeret: {}", e.getMessage());
            return Collections.emptySet();
        }

        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(lagPersonIdent(personIdent.getIdent()));
        request.getInformasjonsbehov().add(Informasjonsbehov.FAMILIERELASJONER);
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            return person.getHarFraRolleI().stream()
                .filter(rel -> rel.getTilRolle().getValue().matches("MORA") || rel.getTilRolle().getValue().matches("FARA"))
                .map(this::mapTilAktørId)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        } catch (HentPersonPersonIkkeFunnet e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            return Collections.emptySet();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw PersonFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

    @Override
    public boolean erRegistrert(AktørId aktørId) {
        PersonIdent personIdent;
        try {
            personIdent = hentIdentForAktørId(aktørId);
        } catch (TekniskException e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            LOGGER.info("Feilet ved kall til aktør-registeret: {}", e.getMessage());
            return false;
        }

        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(lagPersonIdent(personIdent.getIdent()));
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            return response.getPerson() != null;
        } catch (HentPersonPersonIkkeFunnet e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            return false;
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw PersonFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

    @Override
    public boolean harRegistrertDødsdato(AktørId aktørId) {
        PersonIdent personIdent;
        try {
            personIdent = hentIdentForAktørId(aktørId);
        } catch (TekniskException e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            LOGGER.info("Feilet ved kall til aktør-registeret: {}", e.getMessage());
            return false;
        }

        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(lagPersonIdent(personIdent.getIdent()));
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            return person.getDoedsdato() != null && person.getDoedsdato().getDoedsdato() != null;
        } catch (HentPersonPersonIkkeFunnet e) {
            throw PersonFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw PersonFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

    @Override
    public boolean harRegistrertDødfødsel(AktørId aktørId, LocalDate hendelseDato) {
        PersonIdent personIdent;
        try {
            personIdent = hentIdentForAktørId(aktørId);
        } catch (TekniskException e) {
            // Denne er ventet pga forsinkelse TPS vs PDL/aktør, ingen reaksjon
            LOGGER.info("Feilet ved kall til aktør-registeret: {}", e.getMessage());
            return false;
        }

        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(lagPersonIdent(personIdent.getIdent()));
        request.getInformasjonsbehov().add(Informasjonsbehov.FAMILIERELASJONER);
        try {
            HentPersonResponse response = personConsumer.hentPersonResponse(request);
            Person person = response.getPerson();
            return person.getHarFraRolleI().stream()
                .filter(rel -> rel.getTilRolle().getValue().matches("BARN"))
                .anyMatch(rel -> erDødfødselInnenforToUker(rel, hendelseDato));
        } catch (HentPersonPersonIkkeFunnet e) {
            throw PersonFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw PersonFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

    private PersonIdent hentIdentForAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new)
                .orElseThrow(() -> PersonFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException());
    }

    private Optional<AktørId> mapTilAktørId(Familierelasjon familierelasjon) {
        String identNr = ((no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent) familierelasjon.getTilPerson().getAktoer()).getIdent().getIdent();
        PersonIdent ident = PersonIdent.fra(identNr);
        if (ident.erFdatNummer()) {
            return Optional.empty();
        }
        try {
            return aktørConsumer.hentAktørIdForPersonIdent(ident.getIdent()).map(AktørId::new);
        } catch (Exception e) { // NOSONAR
            LOGGER.info("Feilet ved kall til aktør-registeret: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean erDødfødselInnenforToUker(Familierelasjon familierelasjon, LocalDate hendelseDato) {
        String identNr = ((no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent) familierelasjon.getTilPerson().getAktoer()).getIdent().getIdent();
        PersonIdent ident = PersonIdent.fra(identNr);
        if (!ident.erFdatNummer())
            return false;
        DateTimeFormatter identFormatter = DateTimeFormatter.ofPattern("ddMMyy");
        var registrertDato = LocalDate.parse(ident.getIdent().substring(0, 6), identFormatter);
        return hendelseDato.minusDays(8).isBefore(registrertDato) && hendelseDato.plusDays(8).isAfter(registrertDato);
    }

    private no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent lagPersonIdent(String fnr) {
        if (fnr == null || fnr.isEmpty()) {
            throw new IllegalArgumentException("Fødselsnummer kan ikke være null eller tomt");
        }

        no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent personIdent = new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(fnr);

        Personidenter type = new Personidenter();
        type.setValue(erDNr(fnr) ? "DNR" : "FNR");
        norskIdent.setType(type);

        personIdent.setIdent(norskIdent);
        return personIdent;
    }

    private static boolean erDNr(String fnr) {
        //D-nummer kan indentifiseres ved at første siffer er 4 større enn hva som finnes i fødselsnumre
        char førsteTegn = fnr.charAt(0);
        return førsteTegn >= '4' && førsteTegn <= '7';
    }

}
