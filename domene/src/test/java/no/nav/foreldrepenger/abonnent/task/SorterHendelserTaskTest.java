package no.nav.foreldrepenger.abonnent.task;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.lagAktørIdIdent;
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
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.Meldingstype;
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

    private static final FeedEntry FMELDING_1 = lagFødselsmelding(of(lagAktørIdIdent("1112345678909")),
            of(lagAktørIdIdent("1212345678909")), of(lagAktørIdIdent("1312345678909")), FØDSELSDATO);
    private static final FeedEntry FMELDING_2 = lagFødselsmelding(of(lagAktørIdIdent("2112345678909")),
            of(lagAktørIdIdent("1212345678909")), of(lagAktørIdIdent("1312345678909")), FØDSELSDATO);
    private static final FeedEntry FMELDING_3 = lagFødselsmelding(of(lagAktørIdIdent("3112345678909")),
            of(lagAktørIdIdent("3212345678909")), of(lagAktørIdIdent("1312345678909")), FØDSELSDATO);
    private static final FeedEntry FMELDING_4 = lagFødselsmelding(of(lagAktørIdIdent("4112345678909")),
            of(lagAktørIdIdent("3312345678909")), null, FØDSELSDATO);
    private static final FeedEntry FMELDING_5 = lagFødselsmelding(of(lagAktørIdIdent("4212345678909")), null,
            of(lagAktørIdIdent("1412345678909")), FØDSELSDATO);

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
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-690327");

        sorterHendelserTask.doTask(prosessTaskData);
    }

    @Test
    public void skalIkkeOppretteTaskNårIngenHendelserKommerInn() {
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(Collections.emptyList());

        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        verify(mockProsessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void skalIkkeOppretteNyProsessTaskNårGrovsorteringReturnererTomListe() {
        // Arrange
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();
        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(Collections.emptyList());

        InngåendeHendelse hendelse1 = lagInngåendeTPSHendelse(FMELDING_1, 1);
        InngåendeHendelse hendelse2 = lagInngåendeTPSHendelse(FMELDING_4, 4);
        InngåendeHendelse hendelse3 = lagInngåendeTPSHendelse(FMELDING_5, 5);
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
    public void skalOppretteEnSendHendelseTask() {
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        List<String> eksisterendeAktørIder = Arrays.asList("1212345678909", "1312345678909");

        InngåendeHendelse hendelse = lagInngåendeTPSHendelse(FMELDING_1, 1);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        verify(mockProsessTaskRepository).lagre(argumentCaptor.capture());
        HendelserDataWrapper data = new HendelserDataWrapper(argumentCaptor.getValue());
        assertThat(data.getHendelseRequestUuid()).isEqualTo(REQ_UUID);
        assertThat(data.getHendelseType()).isPresent().hasValue(Meldingstype.FOEDSELSMELDINGOPPRETTET.name());
        assertThat(data.getAktørIdBarn()).isPresent();
        assertThat(data.getAktørIdBarn().get()).contains("1112345678909");
        assertThat(data.getAktørIdMor()).isPresent();
        assertThat(data.getAktørIdMor().get()).contains("1212345678909");
        assertThat(data.getAktørIdFar()).isPresent();
        assertThat(data.getAktørIdFar().get()).contains("1312345678909");
        assertThat(data.getFødselsdato()).isPresent().hasValue(FØDSELSDATO.toString());
    }

    @Test
    public void skalOppretteToSendHendelseTaskerNårTvillingerBlirFødt() {
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        List<String> eksisterendeAktørIder = Arrays.asList("1234567890123", "1312345678909");

        InngåendeHendelse hendelse1 = lagInngåendeTPSHendelse(FMELDING_1, 1);
        InngåendeHendelse hendelse2 = lagInngåendeTPSHendelse(FMELDING_2, 2); // Samme foreldre
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

        verify(mockProsessTaskRepository, times(2)).lagre(argumentCaptor.capture());
        List<ProsessTaskData> sendt = argumentCaptor.getAllValues();
        assertThat(sendt).hasSize(2);
    }

    @Test
    public void skalOppretteToSendHendelseTaskerNårFarErIToSeparateHendelser() {
        HendelserDataWrapper dataWrapper = lagDefaultDataWrapper();

        List<String> eksisterendeAktørIder = Arrays.asList("1212345678909", "3212345678909", "1312345678909");

        InngåendeHendelse hendelse1 = lagInngåendeTPSHendelse(FMELDING_1, 1);
        InngåendeHendelse hendelse2 = lagInngåendeTPSHendelse(FMELDING_3, 3);

        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        when(mockHendelseConsumer.grovsorterAktørIder(anyList())).thenReturn(eksisterendeAktørIder);

        ArgumentCaptor<ProsessTaskData> argumentCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);

        sorterHendelserTask.doTask(dataWrapper.getProsessTaskData());

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

        InngåendeHendelse hendelse = lagInngåendeTPSHendelse(FMELDING_5, 1);
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

    private InngåendeHendelse lagInngåendeTPSHendelse(FeedEntry feedEntry, long id) {
        return InngåendeHendelse.builder()
                .hendelseId("" + id)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload(JsonMapper.toJson(feedEntry))
                .feedKode(FeedKode.TPS)
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
