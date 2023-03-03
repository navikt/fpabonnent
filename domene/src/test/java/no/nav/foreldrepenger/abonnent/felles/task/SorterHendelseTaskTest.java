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

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.extensions.JpaExtension;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelserKlient;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class SorterHendelseTaskTest {

    private static final LocalDate FØDSELSDATO = LocalDate.of(2018, 1, 25);

    private static final String HENDELSE_ID = "1";
    private static final String BARNET = "1112345678909";
    private static final String FORELDER1 = "1212345678909";
    private static final String FORELDER2 = "1312345678909";
    private static final PdlFødsel FMELDING = lagFødselsmelding(HENDELSE_ID, of(BARNET), of(FORELDER1, FORELDER2), FØDSELSDATO);

    private EntityManager entityManager;
    private HendelseRepository hendelseRepository;
    private SorterHendelseTask sorterHendelseTask;
    private ProsessTaskData prosessTaskData;

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Mock
    private HendelserKlient hendelser;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @BeforeEach
    void setup(EntityManager em) {
        this.entityManager = em;
        this.hendelseRepository = new HendelseRepository(em);
        this.inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository,
            new HendelseTjenesteProvider(new UnitTestLookupInstanceImpl<>(new PdlFødselHendelseTjeneste())));
        sorterHendelseTask = new SorterHendelseTask(prosessTaskTjeneste, inngåendeHendelseTjeneste, hendelser);
        prosessTaskData = ProsessTaskData.forProsessTask(SorterHendelseTask.class);
    }

    @Test
    void skal_kaste_teknisk_exception_hvis_påkreved_parameter_mangler() {
        // Act
        assertThrows(TekniskException.class, () -> sorterHendelseTask.doTask(prosessTaskData));
    }

    @Test
    void skal_ikke_opprette_task_når_ingen_hendelser_kommer_inn() {
        // Arrange
        var dataWrapper = lagDefaultDataWrapper();
        lenient().when(hendelser.grovsorterAktørIder(anyList())).thenReturn(List.of());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_SendHendelseTask_når_grovsortering_returnerer_tom_liste() {
        // Arrange
        when(hendelser.grovsorterAktørIder(anyList())).thenReturn(List.of());

        var hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        var dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
        var inngåendeHendelse = finnHendelseMedHendelseId();
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    @Test
    void skal_opprette_SendHendelseTask() {
        // Arrange
        List<String> eksisterendeAktørIder = List.of(FORELDER1, FORELDER2);

        var hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        var dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        when(hendelser.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        var argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskTjeneste).lagre(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().taskType()).isEqualTo(TaskType.forProsessTask(SendHendelseTask.class));
        var data = new HendelserDataWrapper(argumentCaptor.getValue());
        assertThat(data.getHendelseType()).isPresent().hasValue(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(data.getHendelseId()).isPresent().hasValue(FMELDING.getHendelseId());
        var inngåendeHendelse = finnHendelseMedHendelseId();
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.GROVSORTERT);
        assertThat(data.getInngåendeHendelseId()).isPresent().hasValue(inngåendeHendelse.getId());
    }

    @Test
    void skal_ikke_opprette_SendHendelseTask_for_ikke_relevant_aktørid() {
        // Arrange
        var eksisterendeAktørIder = List.of("12", "13");
        when(hendelser.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        var hendelse = lagInngåendeHendelse();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelse);

        var dataWrapper = lagDefaultDataWrapper();
        dataWrapper.setInngåendeHendelseId(hendelse.getId());

        // Act
        sorterHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
        var inngåendeHendelse = finnHendelseMedHendelseId();
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    private HendelserDataWrapper lagDefaultDataWrapper() {
        var dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseId(HENDELSE_ID);
        return dataWrapper;
    }

    private InngåendeHendelse lagInngåendeHendelse() {
        return InngåendeHendelse.builder()
            .hendelseId(FMELDING.getHendelseId())
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .payload(DefaultJsonMapper.toJson(FMELDING))
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.SENDT_TIL_SORTERING)
            .build();
    }

    InngåendeHendelse finnHendelseMedHendelseId() {
        var query = entityManager.createQuery("from InngåendeHendelse where hendelseId = :hendelseId ",
            InngåendeHendelse.class); //$NON-NLS-1$
        query.setParameter("hendelseId", SorterHendelseTaskTest.HENDELSE_ID);
        return query.getSingleResult();
    }
}
