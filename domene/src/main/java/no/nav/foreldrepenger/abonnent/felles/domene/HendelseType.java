package no.nav.foreldrepenger.abonnent.felles.domene;

import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseEndringType.ANNULLERT;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseEndringType.KORRIGERT;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseEndringType.OPPHOERT;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseEndringType.OPPRETTET;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType.PDL_DØDFØDSEL_HENDELSE;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType.PDL_DØD_HENDELSE;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType.PDL_FØDSEL_HENDELSE;
import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType.PDL_UTFLYTTING_HENDELSE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HendelseType implements Kodeverdi {

    PDL_FØDSEL_OPPRETTET("PDL_FOEDSEL_OPPRETTET", PDL_FØDSEL_HENDELSE, OPPRETTET),
    PDL_FØDSEL_KORRIGERT("PDL_FOEDSEL_KORRIGERT", PDL_FØDSEL_HENDELSE, KORRIGERT),
    PDL_FØDSEL_ANNULLERT("PDL_FOEDSEL_ANNULLERT", PDL_FØDSEL_HENDELSE, ANNULLERT),
    PDL_FØDSEL_OPPHØRT("PDL_FOEDSEL_OPPHOERT", PDL_FØDSEL_HENDELSE, OPPHOERT),

    PDL_DØD_OPPRETTET("PDL_DOED_OPPRETTET", PDL_DØD_HENDELSE, OPPRETTET),
    PDL_DØD_KORRIGERT("PDL_DOED_KORRIGERT", PDL_DØD_HENDELSE, KORRIGERT),
    PDL_DØD_ANNULLERT("PDL_DOED_ANNULLERT", PDL_DØD_HENDELSE, ANNULLERT),
    PDL_DØD_OPPHØRT("PDL_DOED_OPPHOERT", PDL_DØD_HENDELSE, OPPHOERT),

    PDL_DØDFØDSEL_OPPRETTET("PDL_DOEDFOEDSEL_OPPRETTET", PDL_DØDFØDSEL_HENDELSE, OPPRETTET),
    PDL_DØDFØDSEL_KORRIGERT("PDL_DOEDFOEDSEL_KORRIGERT", PDL_DØDFØDSEL_HENDELSE, KORRIGERT),
    PDL_DØDFØDSEL_ANNULLERT("PDL_DOEDFOEDSEL_ANNULLERT", PDL_DØDFØDSEL_HENDELSE, ANNULLERT),
    PDL_DØDFØDSEL_OPPHØRT("PDL_DOEDFOEDSEL_OPPHOERT", PDL_DØDFØDSEL_HENDELSE, OPPHOERT),

    PDL_UTFLYTTING_OPPRETTET("PDL_UTFLYTTING_OPPRETTET", PDL_UTFLYTTING_HENDELSE, OPPRETTET),
    PDL_UTFLYTTING_KORRIGERT("PDL_UTFLYTTING_KORRIGERT", PDL_UTFLYTTING_HENDELSE, KORRIGERT),
    PDL_UTFLYTTING_ANNULLERT("PDL_UTFLYTTING_ANNULLERT", PDL_UTFLYTTING_HENDELSE, ANNULLERT),
    PDL_UTFLYTTING_OPPHØRT("PDL_UTFLYTTING_OPPHOERT", PDL_UTFLYTTING_HENDELSE, OPPHOERT),
    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-"),
    ;

    private static final Map<String, HendelseType> KODER = new LinkedHashMap<>();

    @JsonValue
    private String kode;

    private HendelseOpplysningType opplysningType;
    private HendelseEndringType endringType;

    HendelseType() {
        // Hibernate trenger den
    }

    HendelseType(String kode) {
        this(kode, HendelseOpplysningType.UDEFINERT, HendelseEndringType.UDEFINERT);
    }

    HendelseType(String kode, HendelseOpplysningType opplysningType, HendelseEndringType endringType) {
        this.kode = kode;
        this.opplysningType = opplysningType;
        this.endringType = endringType;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HendelseType fraKode(@JsonProperty(value = "kode") String kode) {
        if (kode == null) {
            return null;
        }
        return Optional.ofNullable(KODER.get(kode)).orElseThrow(() -> new IllegalArgumentException("Ukjent Hendelsetype: " + kode));
    }

    @Override
    public String getKode() {
        return kode;
    }

    public HendelseOpplysningType getOpplysningType() {
        return opplysningType;
    }

    public HendelseEndringType getEndringType() {
        return endringType;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static boolean erOpprettetHendelseType(HendelseType hendelseType) {
        return Objects.equals(hendelseType.endringType, OPPRETTET);
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
