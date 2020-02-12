package no.nav.foreldrepenger.abonnent.felles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;

/**
 * Marker type som brukes for å finne hendelsespesifikke CDI-tjenester.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface HendelseTypeRef {

    String FØDSELSMELDINGOPPRETTET = "FOEDSELSMELDINGOPPRETTET";
    String DØDSMELDINGOPPRETTET = "DOEDSMELDINGOPPRETTET";
    String DØDFØDSELOPPRETTET = "DOEDFOEDSELOPPRETTET";

    /**
     * Settes til navn på hendelsen slik den defineres i FeedEntry, eller til YTELSE_HENDELSE.
     */
    String value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class HendelseTypeRefLiteral extends AnnotationLiteral<HendelseTypeRef> implements HendelseTypeRef {

        private String navn;

        public HendelseTypeRefLiteral(HendelseType hendelseType) {
            this.navn = hendelseType.getKode();
        }

        @Override
        public String value() {
            return navn;
        }
    }
}