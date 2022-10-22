package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

import no.nav.vedtak.exception.TekniskException;

public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        TekniskException tekniskException = new TekniskException("FP-299955", String.format("JSON-parsing feil: %s", exception.getMessage()), exception);
        LOG.warn(tekniskException.getMessage());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new FeilDto(tekniskException.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
