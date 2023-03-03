package no.nav.foreldrepenger.abonnent.felles.fpsak;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HendelseConsumerTest {

    @Mock
    private RestClient restKlient;

    private HendelserKlient consumer;

    @BeforeEach
    public void setUp() throws Exception {
        consumer = new HendelserKlient(restKlient);
    }

    @Test
    public void skal_videresende_fødselshendelse() {
        ArgumentCaptor<RestRequest> captorPayload = ArgumentCaptor.forClass(RestRequest.class);
        var hendelse = HendelseTestDataUtil.lagFødselsHendelsePayload();
        consumer.sendHendelse(hendelse);

        verify(restKlient).sendReturnOptional(captorPayload.capture(), any());
        var capturedDto = captorPayload.getValue();
        capturedDto.validateRequest(r -> {
            assertThat(r.uri().toString()).contains("http://localhost:8080/fpsak");
            assertThat(r.bodyPublisher().get().contentLength() > 0).isTrue();

        });
    }

    @Test
    public void skal_returnere_tom_liste() {
        List<String> resultat = consumer.grovsorterAktørIder(List.of());
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_videresende_aktørId_som_dto() {
        var captor = ArgumentCaptor.forClass(RestRequest.class);

        List<String> idList = List.of("1", "2", "3");
        String[] resp = {"1", "2", "3"};

        when(restKlient.send(captor.capture(), any())).thenReturn(resp);

        consumer.grovsorterAktørIder(idList);

        captor.getValue().validateRequest(r -> {
            assertThat(r.bodyPublisher().get().contentLength() > 0).isTrue();
        });
    }

}
