package no.nav.foreldrepenger.abonnent.felles.domene;

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
public enum HendelseKilde implements Kodeverdi {

    // Ligger igjen til evt databaseopprydding:
    @Deprecated
    TPS("JF_TPS"),
    @Deprecated
    INFOTRYGD("JF_INFOTRYGD"),

    PDL("KAFKA_PDL"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-"),
    ;

    public static final String KODEVERK = "HENDELSE_KILDE";

    private static final Map<String, HendelseKilde> KODER = new LinkedHashMap<>();

    private String kode;

    HendelseKilde() {
        // Hibernate trenger den
    }

    private HendelseKilde(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static HendelseKilde fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Fagsystem: " + kode);
        }
        return ad;
    }

    public static HendelseKilde fraKodeDefaultUdefinert(@JsonProperty("kode") String kode) {
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
    public static class KodeverdiConverter implements AttributeConverter<HendelseKilde, String> {
        @Override
        public String convertToDatabaseColumn(HendelseKilde attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HendelseKilde convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
