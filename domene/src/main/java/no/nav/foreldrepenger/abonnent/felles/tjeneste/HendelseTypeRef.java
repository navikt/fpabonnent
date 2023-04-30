package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType;
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

    HendelseOpplysningType value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class HendelseTypeRefLiteral extends AnnotationLiteral<HendelseTypeRef> implements HendelseTypeRef {

        private final HendelseOpplysningType opplysningType;

        public HendelseTypeRefLiteral() {
            opplysningType = HendelseOpplysningType.UDEFINERT;
        }

        public HendelseTypeRefLiteral(HendelseType hendelseType) {
            opplysningType = hendelseType != null ? hendelseType.getOpplysningType() : HendelseOpplysningType.UDEFINERT;
        }

        @Override
        public HendelseOpplysningType value() {
            return opplysningType;
        }

    }
}
