package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;

import no.nav.vedtak.exception.TekniskException;

public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

    @Override
    public Response toResponse(JsonMappingException exception) {
        TekniskException tekniskException = new TekniskException("FP-252294", "JSON-mapping feil", exception);
        LOG.warn(tekniskException.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(tekniskException.getMessage()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
