package no.nav.foreldrepenger.abonnent.task;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.lagFødselsmelding;
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
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
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
    private static final String REQ_UUID = "req_uuid";

    private static final PdlFødsel FMELDING_1 = lagFødselsmelding("1", of("1112345678909"),
            of("1212345678909", "1312345678909"), FØDSELSDATO);
    private static final PdlFødsel FMELDING_2 = lagFødselsmelding("2", of("2112345678909"),
            of("1212345678909", "1312345678909"), FØDSELSDATO);
    private static final PdlFødsel FMELDING_3 = lagFødselsmelding("3", of("3112345678909"),
            of("3212345678909", "1312345678909"), FØDSELSDATO);
    private static final PdlFødsel FMELDING_4 = lagFødselsmelding("4", of("4112345678909"),
            of("3312345678909"), FØDSELSDATO);
    private static final PdlFødsel FMELDING_5 = lagFødselsmelding("5", of("4212345678909"),
            of("1412345678909"), FØDSELSDATO);

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

        InngåendeHendelse hendelse1 = lagInngåendeHendelse(FMELDING_1);
        InngåendeHendelse hendelse2 = lagInngåendeHendelse(FMELDING_4);
        InngåendeHendelse hendelse3 = lagInngåendeHendelse(FMELDING_5);
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        repoRule.getEntityManager().flush();

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
        List<InngåendeHendelse> inngåendeHendelser = finnHendelserMedRequestUUID(REQ_UUID);
        assertThat(inngåendeHendelser.stream().map(InngåendeHendelse::getHåndtertStatus))
                .containsOnly(HåndtertStatusType.HÅNDTERT);
    }

    @Test
    public void skal_opprette_SendHendelseTask() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        List<String> eksisterendeAktørIder = Arrays.asList("1212345678909", "1312345678909");

        InngåendeHendelse hendelse = lagInngåendeHendelse(FMELDING_1);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository).lagre(argumentCaptor.capture());
        HendelserDataWrapper data = new HendelserDataWrapper(argumentCaptor.getValue());
        assertThat(data.getHendelseRequestUuid()).isEqualTo(REQ_UUID);
        assertThat(data.getHendelseType()).isPresent().hasValue(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(data.getAktørIdBarn()).isPresent();
        assertThat(data.getAktørIdBarn().get()).contains("1112345678909");
        assertThat(data.getAktørIdForeldre()).isPresent();
        assertThat(data.getAktørIdForeldre().get()).containsExactlyInAnyOrder("1212345678909", "1312345678909");
        assertThat(data.getFødselsdato()).isPresent().hasValue(FØDSELSDATO.toString());
    }

    @Test
    public void skal_opprette_to_SendHendelseTasker_når_tvillinger_blir_født() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        List<String> eksisterendeAktørIder = Arrays.asList("1234567890123", "1312345678909");

        InngåendeHendelse hendelse1 = lagInngåendeHendelse(FMELDING_1);
        InngåendeHendelse hendelse2 = lagInngåendeHendelse(FMELDING_2); // Samme foreldre
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(2)).lagre(argumentCaptor.capture());
        List<ProsessTaskData> sendt = argumentCaptor.getAllValues();
        assertThat(sendt).hasSize(2);
    }

    @Test
    public void skal_opprette_to_SendHendelseTasker_når_far_er_i_to_separate_hendelser() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        List<String> eksisterendeAktørIder = Arrays.asList("1212345678909", "3212345678909", "1312345678909");

        InngåendeHendelse hendelse1 = lagInngåendeHendelse(FMELDING_1);
        InngåendeHendelse hendelse2 = lagInngåendeHendelse(FMELDING_3);

        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);
        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(2)).lagre(argumentCaptor.capture());
        List<ProsessTaskData> sendt = argumentCaptor.getAllValues();
        assertThat(sendt).hasSize(2);
    }

    @Test
    public void skal_ikke_opprette_SendHendelseTask_for_ikke_relevant_aktørid() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        List<String> eksisterendeAktørIder = Arrays.asList("12", "13");
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        InngåendeHendelse hendelse = lagInngåendeHendelse(FMELDING_5);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        // Act
        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
        List<InngåendeHendelse> inngåendeHendelser = repoRule.getEntityManager()
                .createQuery("from InngåendeHendelse", InngåendeHendelse.class).getResultList();
        assertThat(inngåendeHendelser).hasSize(1);
        assertThat(inngåendeHendelser.get(0).getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }


    private HendelserDataWrapper lagDefaultDataWrapper() {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseRequestUuid(REQ_UUID);
        return dataWrapper;
    }

    private InngåendeHendelse lagInngåendeHendelse(PdlFødsel pdlFødsel) {
        return InngåendeHendelse.builder()
                .hendelseId(pdlFødsel.getHendelseId())
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload(JsonMapper.toJson(pdlFødsel))
                .feedKode(FeedKode.PDL)
                .requestUuid(REQ_UUID)
                .håndtertStatus(HåndtertStatusType.SENDT_TIL_SORTERING)
                .build();
    }

    public List<InngåendeHendelse> finnHendelserMedRequestUUID(String requestUUID) {
        TypedQuery<InngåendeHendelse> query = repoRule.getEntityManager().createQuery(
                "from InngåendeHendelse where requestUuid = :requestUuid ", InngåendeHendelse.class); //$NON-NLS-1$
        query.setParameter("requestUuid", requestUUID);
        return query.getResultList();
    }
}
