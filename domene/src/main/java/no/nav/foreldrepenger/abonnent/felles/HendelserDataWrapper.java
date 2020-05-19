package no.nav.foreldrepenger.abonnent.felles;

import static java.util.Arrays.asList;

import java.sql.Clob;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class HendelserDataWrapper {

    public static final String DEFAULT_BESKRIVELSE = "Hendelseoppgave";

    public static final String HENDELSE_REQUEST_UUID = "hendelse.requestUuid";
    private static final String HENDELSE_ID = "hendelse.sekvensnummer";
    private static final String HENDELSE_TYPE = "hendelse.type";
    private static final String ENDRINGSTYPE = "hendelse.endringstype";
    public static final String AKTØR_ID_MOR = "hendelse.aktoerIdMor";
    public static final String AKTØR_ID_FAR = "hendelse.aktoerIdFar";
    public static final String AKTØR_ID_BARN = "hendelse.aktoerIdBarn";
    private static final String FØDSELSDATO = "hendelse.foedselsdato";
    private static final String DØDSDATO = "hendelse.doedsdato";
    private static final String DØDFØDSELSDATO = "hendelse.doedfoedselsdato";
    private static final String AKTØR_ID = "hendelse.aktoerId";
    private static final String AKTØR_ID_LISTE = "hendelse.aktoerIdListe";
    private static final String FOM = "hendelse.fom";
    private static final String IDENT_DATO = "hendelse.identDato";
    private static final String TYPE_YTELSE = "hendelse.typeYtelse";

    private final ProsessTaskData prosessTaskData;

    public HendelserDataWrapper(ProsessTaskData prosessTaskData) {
        this.prosessTaskData = prosessTaskData;
    }

    public ProsessTaskData getProsessTaskData() {
        return prosessTaskData;
    }

    public HendelserDataWrapper nesteSteg(String stegnavn) {
        ProsessTaskData nesteStegProsessTaskData = new ProsessTaskData(stegnavn);

        String taskSekvensnummer = getProsessTaskData().getSekvens();        
        Long taskSekvens = Long.parseLong(taskSekvensnummer) + 1;
        nesteStegProsessTaskData.setSekvens(taskSekvens.toString());
        HendelserDataWrapper neste = new HendelserDataWrapper(nesteStegProsessTaskData);
        neste.copyData(this);
        return neste;
    }

    private void copyData(HendelserDataWrapper fra) {
        this.addProperties(fra.prosessTaskData.getProperties());
        this.setPayload(fra.prosessTaskData.getPayload());
        this.getProsessTaskData().setGruppe(fra.getProsessTaskData().getGruppe());
    }

    private void addProperties(Properties newProps) {
        prosessTaskData.getProperties().putAll(newProps);
    }

    public void setPayload(String payload) {
        prosessTaskData.setPayload(payload);
    }

    public void setPayload(Clob payload) {
        prosessTaskData.setPayload(payload);
    }

    public Optional<String> getPayloadAsString() {
        return Optional.ofNullable(prosessTaskData.getPayloadAsString());
    }

    public Properties hentAlleProsessTaskVerdier() {
        return prosessTaskData.getProperties();
    }

    public Long getId() {
        return prosessTaskData.getId();
    }

    public String getHendelseRequestUuid() {
        return prosessTaskData.getPropertyValue(HENDELSE_REQUEST_UUID);
    }

    public void setHendelseRequestUuid(String hendelseRequestUuid) {
        prosessTaskData.setProperty(HENDELSE_REQUEST_UUID, hendelseRequestUuid);
    }

    public Optional<String> getHendelseId() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_ID));
    }

    public void setHendelseId(String hendelseId) {
        prosessTaskData.setProperty(HENDELSE_ID, hendelseId);
    }

    public Optional<String> getHendelseType() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_TYPE));
    }

    public void setHendelseType(String type) {
        prosessTaskData.setProperty(HENDELSE_TYPE, type);
    }

    public Optional<String> getEndringstype() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(ENDRINGSTYPE));
    }

    public void setEndringstype(String endringstype) {
        prosessTaskData.setProperty(ENDRINGSTYPE, endringstype);
    }

    public Optional<Set<String>> getAktørIdMor() {
        return getKommaseparertPropertySomSet(AKTØR_ID_MOR);
    }

    public void setAktørIdMor(Set<String> aktørId) {
        setKommaseparertPropertyFraSet(AKTØR_ID_MOR, aktørId);
    }

    public Optional<Set<String>> getAktørIdFar() {
        return getKommaseparertPropertySomSet(AKTØR_ID_FAR);
    }

    public void setAktørIdFar(Set<String> aktørId) {
        setKommaseparertPropertyFraSet(AKTØR_ID_FAR, aktørId);
    }

    public Optional<Set<String>> getAktørIdBarn() {
        return getKommaseparertPropertySomSet(AKTØR_ID_BARN);
    }

    public void setAktørIdBarn(Set<String> aktørId) {
        setKommaseparertPropertyFraSet(AKTØR_ID_BARN, aktørId);
    }

    public Optional<String> getAktørId() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(AKTØR_ID));
    }

    public void setAktørId(String aktørId) {
        prosessTaskData.setProperty(AKTØR_ID, aktørId);
    }

    public Optional<Set<String>> getAktørIdListe() {
        return getKommaseparertPropertySomSet(AKTØR_ID_LISTE);
    }

    public void setAktørIdListe(Set<String> aktørId) {
        setKommaseparertPropertyFraSet(AKTØR_ID_LISTE, aktørId);
    }

    public Optional<LocalDate> getFom() {
        String property =  prosessTaskData.getPropertyValue(FOM);
        if (property != null) {
            if (property.contains(";")) {
                throw new IllegalStateException("Inneholder flere bursdager.");
            }
            return Optional.of(LocalDate.parse(property));
        }
        return Optional.empty();
    }

    public void setFom(LocalDate paramVerdi) {
        prosessTaskData.setProperty(FOM, paramVerdi.toString());
    }

    public Optional<String> getIdentDato() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(IDENT_DATO));
    }

    public void setIdentDato(String paramVerdi) {
        prosessTaskData.setProperty(IDENT_DATO, paramVerdi);
    }

    public Optional<String> getTypeYtelse() {
        String verdi = prosessTaskData.getPropertyValue(TYPE_YTELSE);
        return Optional.ofNullable(verdi);
    }

    public void setTypeYtelse(String paramVerdi) {
        prosessTaskData.setProperty(TYPE_YTELSE, paramVerdi);
    }

    public Optional<String> getFødselsdato() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(FØDSELSDATO));
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        prosessTaskData.setProperty(FØDSELSDATO, fødselsdato != null ? fødselsdato.toString() : null);
    }

    public Optional<String> getDødsdato() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(DØDSDATO));
    }

    public void setDødsdato(LocalDate dødsdato) {
        prosessTaskData.setProperty(DØDSDATO, dødsdato != null ? dødsdato.toString() : null);
    }

    public Optional<String> getDødfødselsdato() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(DØDFØDSELSDATO));
    }

    public void setDødfødselsdato(LocalDate dødfødselsdato) {
        prosessTaskData.setProperty(DØDFØDSELSDATO, dødfødselsdato != null ? dødfødselsdato.toString() : null);
    }

    private void setKommaseparertPropertyFraSet(String property, Set<String> aktørIder) {
        StringJoiner stringJoiner = new StringJoiner(",");
        aktørIder.forEach(stringJoiner::add);
        prosessTaskData.setProperty(property, stringJoiner.toString());
    }

    private Optional<Set<String>> getKommaseparertPropertySomSet(String property) {
        String value = prosessTaskData.getPropertyValue(property);
        if (value != null) {
            return Optional.of(new HashSet<>(asList(value.split(","))));
        } else {
            return Optional.empty();
        }
    }
}
