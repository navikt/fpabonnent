package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;

public class AktørIdTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(AktørIdTjeneste.class);

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private AktørIdTjeneste() {
    }

    public static Set<String> getAktørIderForSortering(HendelsePayload payload) {
        var aktørIderFraPayload = payload.getAktørIderForSortering();
        return filtrerBortUgyldigeAktørIder(aktørIderFraPayload);
    }

    private static Set<String> filtrerBortUgyldigeAktørIder(Set<String> aktørIder) {
        var validator = VALIDATOR_FACTORY.getValidator();
        return aktørIder.stream().filter(a -> erGyldig(a, validator)).collect(Collectors.toSet());
    }

    private static boolean erGyldig(String aktørId, Validator validator) {
        Objects.requireNonNull(aktørId, "aktørId");
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(new AktørIdDto(aktørId));

        if (!constraintViolations.isEmpty()) {
            var feilmelding = byggFeilmelding(constraintViolations);
            LOGGER.warn("Validering av aktørId '{}' feilet: {}.", aktørId, feilmelding);
            return false;
        }
        return true;
    }

    private static String byggFeilmelding(Set<ConstraintViolation<Object>> constraintViolations) {
        var feilmelding = new StringBuilder();
        constraintViolations.forEach(cv -> feilmelding.append(cv.getMessage()));
        return feilmelding.toString();
    }
}
