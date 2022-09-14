package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
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
        ArgumentCaptor<HendelseWrapperDto> captorPayload = ArgumentCaptor.forClass(HendelseWrapperDto.class);
        var hendelse = HendelseTestDataUtil.lagFødselsHendelsePayload();
        consumer.sendHendelse(hendelse);

        verify(restKlient).postString(any(), captor.capture(), captorPayload.capture());
        var capturedDto = captor.getValue();
        assertThat(capturedDto.toString()).contains("http://localhost:8080/fpsak");
        assertThat(captorPayload.getValue().getHendelse().getHendelsetype()).isEqualTo(hendelse.mapPayloadTilDto().getHendelse().getHendelsetype());
    }

    @Test
    public void skal_returnere_tom_liste() {
        List<String> resultat = consumer.grovsorterAktørIder(List.of());
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_videresende_aktørId_som_dto() {
        var captor = ArgumentCaptor.forClass(List.class);

        List<String> idList = List.of("1", "2", "3");
        String[] resp = { "1", "2", "3"};

        when(restKlient.postValue(any(), any(), any(), captor.capture(), any())).thenReturn(resp);

        consumer.grovsorterAktørIder(idList);

        var capturedDtoList = captor.getValue();
        assertThat(capturedDtoList).isNotNull();
        assertThat(capturedDtoList).contains(new AktørIdDto("2"));
    }

}
