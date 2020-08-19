package no.nav.foreldrepenger.abonnent.felles.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class SendHendelseTaskTest {

    private static final HendelseType HENDELSE_TYPE = HendelseType.PDL_FØDSEL_OPPRETTET;
    private static final LocalDate FØDSELSDATO = LocalDate.parse("2018-01-01");
    private static final String HENDELSE_ID = "1";
    private static final long INNGÅENDE_HENDELSE_ID = 1L;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseConsumer mockHendelseConsumer;
    private ProsessTaskData prosessTaskData;
    private HendelseRepository hendelseRepository;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    private SendHendelseTask sendHendelseTask;

    @Before
    public void setup() {
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste fødselHendelseTjeneste = new PdlFødselHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(eq(HENDELSE_TYPE), anyString())).thenReturn(fødselHendelseTjeneste);

        mockHendelseConsumer = mock(HendelseConsumer.class);
        hendelseRepository = mock(HendelseRepository.class);
        inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository, hendelseTjenesteProvider);
        sendHendelseTask = new SendHendelseTask(mockHendelseConsumer, inngåendeHendelseTjeneste, hendelseRepository);

        prosessTaskData = new ProsessTaskData(SendHendelseTask.TASKNAME);
        prosessTaskData.setSekvens("1");
    }

    @Test
    public void skal_sende_fødselshendelse() {
        // Arrange
        PdlFødsel.Builder fødselBuilder = PdlFødsel.builder();
        fødselBuilder.medHendelseId(HENDELSE_ID);
        fødselBuilder.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        fødselBuilder.medEndringstype(PdlEndringstype.OPPRETTET);
        fødselBuilder.leggTilPersonident("1111111111111");
        fødselBuilder.leggTilPersonident("2222222222222");
        fødselBuilder.leggTilPersonident("77777777777"); //fnr
        fødselBuilder.medFødselsdato(FØDSELSDATO);
        PdlFødsel fødsel = fødselBuilder.build();
        fødsel.setAktørIdForeldre(Set.of("3333333333333", "4444444444444", "5555555555555", "6666666666666"));
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
                .id(INNGÅENDE_HENDELSE_ID)
                .hendelseId(HENDELSE_ID)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .payload(JsonMapper.toJson(fødsel))
                .build();
        when(hendelseRepository.finnEksaktHendelse(1L)).thenReturn(inngåendeHendelse);
        when(hendelseRepository.finnGrovsortertHendelse(HendelseKilde.PDL, HENDELSE_ID)).thenReturn(Optional.of(inngåendeHendelse));

        HendelserDataWrapper hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setInngåendeHendelseId(INNGÅENDE_HENDELSE_ID);
        hendelse.setHendelseId(HENDELSE_ID);
        hendelse.setHendelseType(HENDELSE_TYPE.getKode());

        ArgumentCaptor<PdlFødselHendelsePayload> payloadCaptor = ArgumentCaptor.forClass(PdlFødselHendelsePayload.class);
        ArgumentCaptor<InngåendeHendelse> ihCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        verify(mockHendelseConsumer, times(1)).sendHendelse(payloadCaptor.capture());
        PdlFødselHendelsePayload payload = payloadCaptor.getValue();
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getHendelseType()).isEqualTo(HENDELSE_TYPE.getKode());
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdBarn().get()).containsExactlyInAnyOrder("1111111111111", "2222222222222");
        assertThat(payload.getAktørIdForeldre()).isPresent();
        assertThat(payload.getAktørIdForeldre().get()).containsExactlyInAnyOrder("3333333333333", "4444444444444", "5555555555555", "6666666666666");
        assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);

        verify(hendelseRepository, times(1)).markerHendelseSomSendtNå(ihCaptor.capture());
        verify(hendelseRepository, times(1)).oppdaterHåndtertStatus(ihCaptor.capture(), eq(HåndtertStatusType.HÅNDTERT));
    }

    @Test
    public void skal_kaste_feil_for_ukjent_inngående_hendelse() {
        // Arrange
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseId(HENDELSE_ID);
        dataWrapper.setInngåendeHendelseId(null);

        // Assert
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-144656");

        // Act
        sendHendelseTask.doTask(dataWrapper.getProsessTaskData());
    }
}
