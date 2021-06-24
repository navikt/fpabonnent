package no.nav.foreldrepenger.abonnent.felles.domene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HendelseType implements Kodeverdi {

    // Ikke lengre støttet, men beholdes så lenge det finnes data i tabeller:
    @Deprecated
    FØDSELSMELDINGOPPRETTET("FOEDSELSMELDINGOPPRETTET"),
    @Deprecated
    DØDSMELDINGOPPRETTET("DOEDSMELDINGOPPRETTET"),
    @Deprecated
    DØDFØDSELOPPRETTET("DOEDFOEDSELOPPRETTET"),
    @Deprecated
    OPPHØERT("OPPHOERT_v1"),
    @Deprecated
    INNVILGET("INNVILGET_v1"),
    @Deprecated
    ANNULLERT("ANNULLERT_v1"),
    @Deprecated
    ENDRET("ENDRET_v1"),

    PDL_FØDSEL_OPPRETTET("PDL_FOEDSEL_OPPRETTET"),
    PDL_FØDSEL_KORRIGERT("PDL_FOEDSEL_KORRIGERT"),
    PDL_FØDSEL_ANNULLERT("PDL_FOEDSEL_ANNULLERT"),
    PDL_FØDSEL_OPPHØRT("PDL_FOEDSEL_OPPHOERT"),

    PDL_DØD_OPPRETTET("PDL_DOED_OPPRETTET"),
    PDL_DØD_KORRIGERT("PDL_DOED_KORRIGERT"),
    PDL_DØD_ANNULLERT("PDL_DOED_ANNULLERT"),
    PDL_DØD_OPPHØRT("PDL_DOED_OPPHOERT"),

    PDL_DØDFØDSEL_OPPRETTET("PDL_DOEDFOEDSEL_OPPRETTET"),
    PDL_DØDFØDSEL_KORRIGERT("PDL_DOEDFOEDSEL_KORRIGERT"),
    PDL_DØDFØDSEL_ANNULLERT("PDL_DOEDFOEDSEL_ANNULLERT"),
    PDL_DØDFØDSEL_OPPHØRT("PDL_DOEDFOEDSEL_OPPHOERT"),

    PDL_UTFLYTTING_OPPRETTET("PDL_UTFLYTTING_OPPRETTET"),
    PDL_UTFLYTTING_KORRIGERT("PDL_UTFLYTTING_KORRIGERT"),
    PDL_UTFLYTTING_ANNULLERT("PDL_UTFLYTTING_ANNULLERT"),
    PDL_UTFLYTTING_OPPHØRT("PDL_UTFLYTTING_OPPHOERT"),

    // Prosesseres ikke pt: - sjekk forekomst i database før evt slettes herfra
    PDL_FAMILIERELASJON_OPPRETTET("PDL_FAMILIERELASJON_OPPRETTET"),
    PDL_FAMILIERELASJON_KORRIGERT("PDL_FAMILIERELASJON_KORRIGERT"),
    PDL_FAMILIERELASJON_ANNULLERT("PDL_FAMILIERELASJON_ANNULLERT"),
    PDL_FAMILIERELASJON_OPPHØRT("PDL_FAMILIERELASJON_OPPHOERT"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-"),
    ;

    public static final String KODEVERK = "HENDELSE_TYPE";

    private static final Map<String, HendelseType> KODER = new LinkedHashMap<>();

    private String kode;

    HendelseType() {
        // Hibernate trenger den
    }

    private HendelseType(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static HendelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Fagsystem: " + kode);
        }
        return ad;
    }

    public static HendelseType fraKodeDefaultUdefinert(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return KODER.getOrDefault(kode, UDEFINERT);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static boolean erPdlFødselHendelseType(HendelseType hendelseType) {
        return Set.of(PDL_FØDSEL_OPPRETTET, PDL_FØDSEL_KORRIGERT, PDL_FØDSEL_ANNULLERT, PDL_FØDSEL_OPPHØRT).contains(hendelseType);
    }

    public static boolean erPdlDødHendelseType(HendelseType hendelseType) {
        return Set.of(PDL_DØD_OPPRETTET, PDL_DØD_KORRIGERT, PDL_DØD_ANNULLERT, PDL_DØD_OPPHØRT).contains(hendelseType);
    }

    public static boolean erPdlDødfødselHendelseType(HendelseType hendelseType) {
        return Set.of(PDL_DØDFØDSEL_OPPRETTET, PDL_DØDFØDSEL_KORRIGERT, PDL_DØDFØDSEL_ANNULLERT, PDL_DØDFØDSEL_OPPHØRT).contains(hendelseType);
    }

    public static boolean erPdlUtflyttingHendelseType(HendelseType hendelseType) {
        return Set.of(PDL_UTFLYTTING_OPPRETTET, PDL_UTFLYTTING_KORRIGERT, PDL_UTFLYTTING_ANNULLERT, PDL_UTFLYTTING_OPPHØRT).contains(hendelseType);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HendelseType, String> {
        @Override
        public String convertToDatabaseColumn(HendelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HendelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
