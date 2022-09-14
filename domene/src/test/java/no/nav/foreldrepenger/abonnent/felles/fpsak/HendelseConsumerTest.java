package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.vedtak.felles.integrasjon.rest.RestCompact;

@ExtendWith(MockitoExtension.class)
public class HendelseConsumerTest {

    @Mock
    private RestCompact restKlient;

    private Hendelser consumer;

    @BeforeEach
    public void setUp() throws Exception {
        consumer = new NativeHendelser(restKlient);
    }

    @Test
    public void skal_videresende_fødselshendelse() {
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        consumer.sendHendelse(HendelseTestDataUtil.lagFødselsHendelsePayload());

        verify(restKlient).postString(any(), captor.capture(), eq(HendelseTestDataUtil.lagFødselsHendelsePayload().mapPayloadTilDto()));
        var capturedDto = captor.getValue();
        assertThat(capturedDto.toString()).contains("http://localhost:8080/fpsak");
    }

    @Test
    public void skal_returnere_tom_liste() {
        List<String> resultat = consumer.grovsorterAktørIder(List.of());
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_videresende_aktørId_som_dto() {
        var captor = ArgumentCaptor.forClass(String[].class);

        List<String> idList = List.of("1", "2", "3");

        consumer.grovsorterAktørIder(idList);

        //verify(restKlient).send(any(), captor.capture());
        var capturedDtoList = captor.getValue();
        assertThat(capturedDtoList).isNotNull();
        assertThat(capturedDtoList).extracting("aktørId").contains("1", "2", "3");
    }

}
