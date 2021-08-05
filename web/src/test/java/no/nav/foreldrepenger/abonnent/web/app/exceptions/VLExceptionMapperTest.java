package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;

@Execution(ExecutionMode.SAME_THREAD)
public class VLExceptionMapperTest {

    private final VLExceptionMapper vlExceptionMapper = new VLExceptionMapper();

    @Test
    public void skalMappeManglerTilgangFeil() {
        VLException manglerTilgangFeil = new ManglerTilgangException("MANGLER_TILGANG_FEIL", "ManglerTilgangFeilmeldingKode");

        Response response = vlExceptionMapper.toResponse(manglerTilgangFeil);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.type()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
        assertThat(feilDto.feilmelding()).isEqualTo("MANGLER_TILGANG_FEIL:ManglerTilgangFeilmeldingKode");
    }

    @Test
    public void skalMappeFunksjonellFeil() {
        VLException funksjonellFeil = new FunksjonellException("FUNK_FEIL", "en funksjonell feilmelding", "et løsningsforslag");

        Response response = vlExceptionMapper.toResponse(funksjonellFeil);

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.feilmelding()).contains("FUNK_FEIL");
        assertThat(feilDto.feilmelding()).contains("en funksjonell feilmelding");
        assertThat(feilDto.feilmelding()).contains("et løsningsforslag");
    }

    @Test
    public void skalMappeVLException() {
        VLException vlException = new TekniskException("TEK_FEIL", "en teknisk feilmelding");

        Response response = vlExceptionMapper.toResponse(vlException);

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.feilmelding()).contains("TEK_FEIL");
        assertThat(feilDto.feilmelding()).contains("en teknisk feilmelding");
    }

    @Test
    public void skalMappeWrappedGenerellFeil() {
        String feilmelding = "en helt generell feil";
        RuntimeException generellFeil = new RuntimeException(feilmelding);

        Response response = vlExceptionMapper.toResponse(new TekniskException("KODE", "TEKST", generellFeil));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.feilmelding()).contains("TEKST");
    }

    @Test
    public void skalMappeWrappedFeilUtenCause() {
        String feilmelding = "en helt generell feil";

        Response response = vlExceptionMapper.toResponse(new TekniskException("KODE", feilmelding));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        FeilDto feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.feilmelding()).contains(feilmelding);
    }
}
