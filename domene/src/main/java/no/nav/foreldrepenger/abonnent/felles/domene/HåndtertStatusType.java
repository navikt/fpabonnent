package no.nav.foreldrepenger.abonnent.felles.domene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

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

    private static final Map<String, HåndtertStatusType> KODER = new LinkedHashMap<>();

    @JsonValue
    private String kode;

    HåndtertStatusType() {
        // Hibernate trenger den
    }

    HåndtertStatusType(String kode) {
        this.kode = kode;
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
    public static class KodeverdiConverter implements AttributeConverter<HåndtertStatusType, String> {
        @Override
        public String convertToDatabaseColumn(HåndtertStatusType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HåndtertStatusType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static HåndtertStatusType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            return Optional.ofNullable(KODER.get(kode)).orElseThrow(() -> new IllegalArgumentException("Ukjent HåndtertStatusType: " + kode));
        }
    }
}
