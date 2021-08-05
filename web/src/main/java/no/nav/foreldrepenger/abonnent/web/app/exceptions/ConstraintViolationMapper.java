package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger log = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Collection<FeltFeilDto> feilene = new ArrayList<>();

        Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            String feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage()));
        }
        List<String> feltNavn = feilene.stream().map(FeltFeilDto::navn).collect(Collectors.toList());

        var feil = FeltValideringFeil.feltverdiKanIkkeValideres(feltNavn);
        log.warn(feil.getMessage());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new FeilDto(feil.getMessage(), feilene))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof PathImpl pi ? pi.getLeafNode().toString() : null;
    }

}
