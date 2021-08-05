package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class ThrowableExceptionMapperTest {

    private final ThrowableExceptionMapper exceptionMapper = new ThrowableExceptionMapper();

    @Test
    public void skalMappeGenerellFeil() {
        String feilmelding = "en helt generell feil";
        RuntimeException generellFeil = new IllegalArgumentException(feilmelding);

        Response response = exceptionMapper.toResponse(generellFeil);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.feilmelding()).contains(feilmelding);
    }
}
