package no.nav.foreldrepenger.abonnent.felles.task;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.lagFødselsmelding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import no.nav.foreldrepenger.abonnent.extensions.CdiDbAwareTest;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@CdiDbAwareTest
public class SorterHendelseTaskTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }

    private static final String PROSESSTASK_STEG = "hendelser.grovsorter";

    private static final LocalDate FØDSELSDATO = LocalDate.of(2018, 1, 25);

    private static final String HENDELSE_ID = "1";
    private static final String BARNET = "1112345678909";
    private static final String FORELDER1 = "1212345678909";
    private static final String FORELDER2 = "1312345678909";
    private static final PdlFødsel FMELDING = lagFødselsmelding(HENDELSE_ID, of(BARNET), of(FORELDER1, FORELDER2), FØDSELSDATO);

    @Inject
    private EntityManager entityManager;

    @Inject
    private HendelseRepository hendelseRepository;
    private SorterHendelseTask sorterHendelseTask;
    private ProsessTaskData prosessTaskData;

    @Inject
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Mock
    private HendelseConsumer mockHendelseConsumer;
    @Mock
    private ProsessTaskRepository mockProsessTaskRepository;

    @BeforeEach
    public void setup() {
        sorterHendelseTask = new SorterHendelseTask(mockProsessTaskRepository, inngåendeHendelseTjeneste,
                mockHendelseConsumer);

        prosessTaskData = new ProsessTaskData(PROSESSTASK_STEG);
        prosessTaskData.setSekvens("1");
    }

    @Test
    public void skal_kaste_teknisk_exception_hvis_påkreved_parameter_mangler() {

        // Act
        assertThrows(TekniskException.class, () -> sorterHendelseTask.doTask(prosessTaskData));
    }

    @Test
    public void skal_ikke_opprette_task_når_ingen_hendelser_kommer_inn() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        lenient().when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(List.of());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void skal_ikke_opprette_SendHendelseTask_når_grovsortering_returnerer_tom_liste() {
        // Arrange
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(List.of());

        InngåendeHendelse hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
        InngåendeHendelse inngåendeHendelse = finnHendelseMedHendelseId(HENDELSE_ID);
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    @Test
    public void skal_opprette_SendHendelseTask() {
        // Arrange
        List<String> eksisterendeAktørIder = List.of(FORELDER1, FORELDER2);

        InngåendeHendelse hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository).lagre(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getTaskType()).isEqualTo(SendHendelseTask.TASKNAME);
        HendelserDataWrapper data = new HendelserDataWrapper(argumentCaptor.getValue());
        assertThat(data.getHendelseType()).isPresent().hasValue(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(data.getHendelseId()).isPresent().hasValue(FMELDING.getHendelseId());
        InngåendeHendelse inngåendeHendelse = finnHendelseMedHendelseId(HENDELSE_ID);
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.GROVSORTERT);
        assertThat(data.getInngåendeHendelseId()).isPresent().hasValue(inngåendeHendelse.getId());
    }

    @Test
    public void skal_ikke_opprette_SendHendelseTask_for_ikke_relevant_aktørid() {
        // Arrange
        List<String> eksisterendeAktørIder = List.of("12", "13");
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        InngåendeHendelse hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
        InngåendeHendelse inngåendeHendelse = finnHendelseMedHendelseId(HENDELSE_ID);
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    private HendelserDataWrapper lagDefaultDataWrapper() {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseId(HENDELSE_ID);
        return dataWrapper;
    }

    private InngåendeHendelse lagInngåendeHendelse() {
        return InngåendeHendelse.builder()
                .hendelseId(FMELDING.getHendelseId())
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload(JsonMapper.toJson(FMELDING))
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.SENDT_TIL_SORTERING)
                .build();
    }

    public InngåendeHendelse finnHendelseMedHendelseId(String hendelseId) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery(
                "from InngåendeHendelse where hendelseId = :hendelseId ", InngåendeHendelse.class); //$NON-NLS-1$
        query.setParameter("hendelseId", hendelseId);
        return query.getSingleResult();
    }
}
