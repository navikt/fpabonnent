package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

@ExtendWith(MockitoExtension.class)
class HendelseConsumerTest {

    @Mock
    private RestClient restKlient;

    private HendelserKlient consumer;

    @BeforeEach
    void setUp() {
        consumer = new HendelserKlient(restKlient);
    }

    @Test
    void skal_videresende_fødselshendelse() {
        var captorPayload = ArgumentCaptor.forClass(RestRequest.class);
        var hendelse = HendelseTestDataUtil.lagFødselsHendelsePayload();
        consumer.sendHendelse(hendelse);

        verify(restKlient).sendReturnOptional(captorPayload.capture(), any());
        var capturedDto = captorPayload.getValue();
        capturedDto.validateRequest(r -> {
            assertThat(r.uri().toString()).contains("8080/fpsak");
            assertThat(r.bodyPublisher().orElseThrow().contentLength()).isPositive();

        });
    }

    @Test
    void skal_returnere_tom_liste() {
        var resultat = consumer.grovsorterAktørIder(Set.of());
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_videresende_aktørId_som_dto() {
        var captor = ArgumentCaptor.forClass(RestRequest.class);

        var idList = Set.of("1", "2", "3");
        var resp = new String[]{"1", "2", "3"};

        when(restKlient.send(captor.capture(), any())).thenReturn(resp);

        consumer.grovsorterAktørIder(idList);

        captor.getValue().validateRequest(r -> {
            assertThat(r.bodyPublisher().orElseThrow().contentLength()).isPositive();
        });
    }

}
