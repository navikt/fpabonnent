package no.nav.foreldrepenger.abonnent.feed.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;

public class AktørIdTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(AktørIdTjeneste.class);

    private static ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    private AktørIdTjeneste() {
    }

    public static List<String> getAktørIderForSortering(List<HendelsePayload> payloadList) {
        List<String> aktørIderFraPayload = new ArrayList<>();
        for (HendelsePayload payload : payloadList) {
            Set<String> aktørIder = payload.getAktørIderForSortering();
            aktørIderFraPayload.addAll(aktørIder);
        }
        return filtrerBortUgyldigeAktørIder(aktørIderFraPayload);
    }

    private static List<String> filtrerBortUgyldigeAktørIder(List<String> aktørIder) {
        Validator validator = validatorFactory.getValidator();
        return aktørIder.stream().filter(a -> erGyldig(a, validator)).collect(Collectors.toList());
    }

    private static boolean erGyldig(String aktørId, Validator validator) {
        Objects.requireNonNull(aktørId, "aktørId");
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(new AktørIdDto(aktørId));

        if (!constraintViolations.isEmpty()) {
            String feilmelding = byggFeilmelding(constraintViolations);
            LOGGER.warn("Validering av aktørId '{}' feilet: {}.", aktørId, feilmelding);
            return false;
        }
        return true;
    }

    private static String byggFeilmelding(Set<ConstraintViolation<Object>> constraintViolations) {
        StringBuilder feilmelding = new StringBuilder();
        constraintViolations.forEach(cv -> feilmelding.append(cv.getMessage()));
        return feilmelding.toString();
    }
}
