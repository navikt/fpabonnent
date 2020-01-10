package no.nav.foreldrepenger.abonnent.kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

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
public enum HåndtertStatusType implements Kodeverdi {

    MOTTATT("MOTTATT"),
    SENDT_TIL_SORTERING("SENDT_TIL_SORTERING"),
    GROVSORTERT("GROVSORTERT"),
    HÅNDTERT("HÅNDTERT"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-"),
    ;

    public static final String KODEVERK = "HAANDTERT_STATUS";

    private static final Map<String, HåndtertStatusType> KODER = new LinkedHashMap<>();

    private String kode;

    HåndtertStatusType() {
        // Hibernate trenger den
    }

    private HåndtertStatusType(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static HåndtertStatusType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Fagsystem: " + kode);
        }
        return ad;
    }

    public static HåndtertStatusType fraKodeDefaultUdefinert(@JsonProperty("kode") String kode) {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HåndtertStatusType, String> {
        @Override
        public String convertToDatabaseColumn(HåndtertStatusType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HåndtertStatusType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
