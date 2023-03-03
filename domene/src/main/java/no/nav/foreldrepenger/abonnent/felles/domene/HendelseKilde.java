package no.nav.foreldrepenger.abonnent.felles.domene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HendelseKilde implements Kodeverdi {

    PDL("KAFKA_PDL"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-"),
    ;

    private static final Map<String, HendelseKilde> KODER = new LinkedHashMap<>();

    @JsonValue
    private String kode;

    HendelseKilde() {
        // Hibernate trenger den
    }

    private HendelseKilde(String kode) {
        this.kode = kode;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

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

        private static HendelseKilde fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            return Optional.ofNullable(KODER.get(kode)).orElseThrow(() -> new IllegalArgumentException("Ukjent HendelseKilde: " + kode));
        }
    }
}
