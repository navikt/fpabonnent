package no.nav.foreldrepenger.abonnent.felles.task;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.felles.HendelseTestDataUtil.lagFødselsmelding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SorterHendelserTaskTest {

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

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());
    private SorterHendelserTask sorterHendelserTask;
    private ProsessTaskData prosessTaskData;

    @Inject
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Inject
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    private HendelseConsumer mockHendelseConsumer;
    private ProsessTaskRepository mockProsessTaskRepository;

    @Before
    public void setup() {
        mockHendelseConsumer = mock(HendelseConsumer.class);
        mockProsessTaskRepository = mock(ProsessTaskRepository.class);

        sorterHendelserTask = new SorterHendelserTask(mockProsessTaskRepository, inngåendeHendelseTjeneste,
                mockHendelseConsumer, hendelseTjenesteProvider);

        prosessTaskData = new ProsessTaskData(PROSESSTASK_STEG);
        prosessTaskData.setSekvens("1");
    }

    @Test
    public void skal_kaste_teknisk_exception_hvis_påkreved_parameter_mangler() {
        // Arrange
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-690327");

        // Act
        sorterHendelserTask.doTask(prosessTaskData);
    }

    @Test
    public void skal_ikke_opprette_task_når_ingen_hendelser_kommer_inn() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(Collections.emptyList());

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void skal_ikke_opprette_SendHendelseTask_når_grovsortering_returnerer_tom_liste() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(Collections.emptyList());

        InngåendeHendelse hendelse1 = lagInngåendeHendelse(FMELDING);
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        repoRule.getEntityManager().flush();

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
        InngåendeHendelse inngåendeHendelse = finnHendelseMedHendelseId(HENDELSE_ID);
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    @Test
    public void skal_opprette_SendHendelseTask() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        List<String> eksisterendeAktørIder = Arrays.asList(FORELDER1, FORELDER2);

        InngåendeHendelse hendelse = lagInngåendeHendelse(FMELDING);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository).lagre(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getTaskType()).isEqualTo(SendHendelseTask.TASKNAME);
        HendelserDataWrapper data = new HendelserDataWrapper(argumentCaptor.getValue());
        assertThat(data.getHendelseType()).isPresent().hasValue(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(data.getAktørIdBarn()).isPresent();
        assertThat(data.getAktørIdBarn().get()).contains(BARNET);
        assertThat(data.getAktørIdForeldre()).isPresent();
        assertThat(data.getAktørIdForeldre().get()).containsExactlyInAnyOrder(FORELDER1, FORELDER2);
        assertThat(data.getFødselsdato()).isPresent().hasValue(FØDSELSDATO.toString());
    }

    @Test
    public void skal_ikke_opprette_SendHendelseTask_for_ikke_relevant_aktørid() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        List<String> eksisterendeAktørIder = Arrays.asList("12", "13");
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        InngåendeHendelse hendelse = lagInngåendeHendelse(FMELDING);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

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

    private InngåendeHendelse lagInngåendeHendelse(PdlFødsel pdlFødsel) {
        return InngåendeHendelse.builder()
                .hendelseId(pdlFødsel.getHendelseId())
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload(JsonMapper.toJson(pdlFødsel))
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.SENDT_TIL_SORTERING)
                .build();
    }

    public InngåendeHendelse finnHendelseMedHendelseId(String hendelseId) {
        TypedQuery<InngåendeHendelse> query = repoRule.getEntityManager().createQuery(
                "from InngåendeHendelse where hendelseId = :hendelseId ", InngåendeHendelse.class); //$NON-NLS-1$
        query.setParameter("hendelseId", hendelseId);
        return query.getSingleResult();
    }
}
