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
public enum HendelseType implements Kodeverdi {

    FØDSELSMELDINGOPPRETTET("FOEDSELSMELDINGOPPRETTET"),
    DØDSMELDINGOPPRETTET("DOEDSMELDINGOPPRETTET"),
    DØDFØDSELOPPRETTET("DOEDFOEDSELOPPRETTET"),
    OPPHØERT("OPPHOERT_v1"),
    INNVILGET("INNVILGET_v1"),
    ANNULLERT("ANNULLERT_v1"),
    ENDRET("ENDRET_v1"),

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

    public static boolean erYtelseHendelseType(HendelseType hendelseType) {
        return OPPHØERT.equals(hendelseType)
                || INNVILGET.equals(hendelseType)
                || ANNULLERT.equals(hendelseType)
                || ENDRET.equals(hendelseType);
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
