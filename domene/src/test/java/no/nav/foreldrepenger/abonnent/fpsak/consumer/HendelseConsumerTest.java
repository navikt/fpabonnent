package no.nav.foreldrepenger.abonnent.fpsak.consumer;

import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.FØDSELSDATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class HendelseConsumerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OidcRestClient oidcRestClient;

    private HendelseConsumer consumer;

    private URI baseEndpoint;
    private URI hendelseEndpoint;
    private URI grovsorterEndpoint;

    @Before
    public void setUp() throws Exception {
        baseEndpoint = new URI("/test");
        hendelseEndpoint = baseEndpoint.resolve("motta");
        grovsorterEndpoint = baseEndpoint.resolve("grovsorter");
        consumer = new HendelseConsumer(oidcRestClient, baseEndpoint);
    }

    @Test
    public void skal_videresende_fødselshendelse() {
        ArgumentCaptor<HendelseWrapperDto> captor = ArgumentCaptor.forClass(HendelseWrapperDto.class);
        consumer.sendHendelse(HendelseTestDataUtil.lagFødselsHendelsePayload());

        verify(oidcRestClient).post(Mockito.eq(hendelseEndpoint), captor.capture());
        HendelseWrapperDto capturedDto = captor.getValue();
        assertThat(capturedDto).isNotNull();

        HendelseDto hendelseDto = capturedDto.getHendelse();
        if (hendelseDto instanceof FødselHendelseDto) {
            FødselHendelseDto fødselHendelseDto = (FødselHendelseDto) hendelseDto;
            assertThat(fødselHendelseDto.getFødselsdato()).isEqualTo(FØDSELSDATO);
            assertThat(fødselHendelseDto.getHendelsetype()).isEqualTo(FødselHendelseDto.HENDELSE_TYPE);
            assertThat(fødselHendelseDto.getAktørIdForeldre()).containsExactlyInAnyOrder(new AktørIdDto(AKTØR_ID_FAR), new AktørIdDto(AKTØR_ID_MOR));
        }
    }

    @Test
    public void skal_returnere_tom_liste() {
        List<String> resultat = consumer.grovsorterAktørIder(Collections.emptyList());
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_videresende_aktørId_som_dto() {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        List<String> idList = Arrays.asList("1", "2", "3");

        consumer.grovsorterAktørIder(idList);

        verify(oidcRestClient).post(Mockito.eq(grovsorterEndpoint), captor.capture(), Mockito.eq(List.class));
        List<List> capturedDtoList = captor.getAllValues();
        assertThat(capturedDtoList).isNotNull();
        assertThat(capturedDtoList.get(0)).extracting("aktørId").contains("1", "2", "3");
    }

}
