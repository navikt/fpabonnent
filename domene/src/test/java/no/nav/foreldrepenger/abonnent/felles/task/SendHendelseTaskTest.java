package no.nav.foreldrepenger.abonnent.felles.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelserKlient;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SendHendelseTaskTest {

    private static final HendelseType HENDELSE_TYPE = HendelseType.PDL_FØDSEL_OPPRETTET;
    private static final LocalDate FØDSELSDATO = LocalDate.parse("2018-01-01");
    private static final String HENDELSE_ID = "1";
    private static final long INNGÅENDE_HENDELSE_ID = 1L;


    private HendelserKlient mockHendelseConsumer;
    private ProsessTaskData prosessTaskData;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    private SendHendelseTask sendHendelseTask;

    @BeforeEach
    void setup() {
        inngåendeHendelseTjeneste = mock(InngåendeHendelseTjeneste.class);
        mockHendelseConsumer = mock(HendelserKlient.class);
        sendHendelseTask = new SendHendelseTask(mockHendelseConsumer, inngåendeHendelseTjeneste);

        prosessTaskData = ProsessTaskData.forProsessTask(SendHendelseTask.class);
    }

    @Test
    void skal_sende_fødselshendelse() {
        // Arrange
        var fødselBuilder = PdlFødsel.builder();
        fødselBuilder.medHendelseId(HENDELSE_ID);
        fødselBuilder.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        fødselBuilder.medEndringstype(PdlEndringstype.OPPRETTET);
        fødselBuilder.leggTilPersonident("1111111111111");
        fødselBuilder.leggTilPersonident("2222222222222");
        fødselBuilder.leggTilPersonident("77777777777"); //fnr
        fødselBuilder.medFødselsdato(FØDSELSDATO);
        var fødsel = fødselBuilder.build();
        fødsel.setAktørIdForeldre(Set.of("3333333333333", "4444444444444", "5555555555555", "6666666666666"));
        var inngåendeHendelse = InngåendeHendelse.builder()
            .id(INNGÅENDE_HENDELSE_ID)
            .hendelseId(HENDELSE_ID)
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.GROVSORTERT)
            .payload(DefaultJsonMapper.toJson(fødsel))
            .build();
        when(inngåendeHendelseTjeneste.finnEksaktHendelse(1L)).thenReturn(inngåendeHendelse);
        when(inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse))
            .thenReturn(new PdlFødselHendelseTjeneste().payloadFraJsonString(inngåendeHendelse.getPayload()));

        var hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setInngåendeHendelseId(INNGÅENDE_HENDELSE_ID);
        hendelse.setHendelseId(HENDELSE_ID);
        hendelse.setHendelseType(HENDELSE_TYPE.getKode());

        var payloadCaptor = ArgumentCaptor.forClass(PdlFødselHendelsePayload.class);
        var ihCaptor = ArgumentCaptor.forClass(HendelsePayload.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        verify(mockHendelseConsumer, times(1)).sendHendelse(payloadCaptor.capture());
        var payload = payloadCaptor.getValue();
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getHendelseType()).isEqualTo(HENDELSE_TYPE.getKode());
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdBarn().get()).containsExactlyInAnyOrder("1111111111111", "2222222222222");
        assertThat(payload.getAktørIdForeldre()).isPresent();
        assertThat(payload.getAktørIdForeldre().get()).containsExactlyInAnyOrder("3333333333333", "4444444444444", "5555555555555", "6666666666666");
        assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);

        verify(inngåendeHendelseTjeneste, times(1)).oppdaterHendelseSomSendtNå(ihCaptor.capture());
    }

    @Test
    void skal_kaste_feil_for_ukjent_inngående_hendelse() {
        // Arrange
        var dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseId(HENDELSE_ID);
        dataWrapper.setInngåendeHendelseId(null);

        var task = dataWrapper.getProsessTaskData();

        // Act
        assertThrows(TekniskException.class, () -> sendHendelseTask.doTask(task));
    }
}
