package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

/**
 * Marker type som brukes for å finne hendelsespesifikke CDI-tjenester.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface HendelseTypeRef {

    String PDL_FØDSEL_HENDELSE = "PDL_FØDSEL_HENDELSE";
    String PDL_DØD_HENDELSE = "PDL_DØD_HENDELSE";
    String PDL_DØDFØDSEL_HENDELSE = "PDL_DØDFØDSEL_HENDELSE";
    String PDL_UTFLYTTING_HENDELSE = "PDL_UTFLYTTING_HENDELSE";

    String value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class HendelseTypeRefLiteral extends AnnotationLiteral<HendelseTypeRef> implements HendelseTypeRef {

        private String navn;

        public HendelseTypeRefLiteral(HendelseType hendelseType) {
            if (HendelseType.erPdlFødselHendelseType(hendelseType)) {
                this.navn = PDL_FØDSEL_HENDELSE;
            } else if (HendelseType.erPdlDødHendelseType(hendelseType)) {
                this.navn = PDL_DØD_HENDELSE;
            } else if (HendelseType.erPdlDødfødselHendelseType(hendelseType)) {
                this.navn = PDL_DØDFØDSEL_HENDELSE;
            } else if (HendelseType.erPdlUtflyttingHendelseType(hendelseType)) {
                this.navn = PDL_UTFLYTTING_HENDELSE;
            } else {
                this.navn = hendelseType.getKode();
            }
        }

        @Override
        public String value() {
            return navn;
        }
    }
}