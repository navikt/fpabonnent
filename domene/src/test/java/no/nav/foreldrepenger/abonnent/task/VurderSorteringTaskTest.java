package no.nav.foreldrepenger.abonnent.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.tps.PdlFødselHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.TpsForsinkelseTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class VurderSorteringTaskTest {

    private static final String AKTØR_ID_BARN = "1111111111111";
    private static final String AKTØR_ID_MOR = "2222222222222";
    private static final String AKTØR_ID_FAR = "3333333333333";
    private static final String HENDELSE_ID = "1";

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private ProsessTaskRepository prosessTaskRepository = mock(ProsessTaskRepository.class);

    @Inject
    private TpsForsinkelseTjeneste tpsForsinkelseTjeneste;

    private PersonTjeneste personTjeneste = mock(PersonTjeneste.class);

    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());

    private VurderSorteringTask vurderSorteringTask;

    @Before
    public void before() {
        HendelseTjeneste hendelseTjeneste = new PdlFødselHendelseTjeneste(personTjeneste);
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);
        vurderSorteringTask = new VurderSorteringTask(prosessTaskRepository, tpsForsinkelseTjeneste, hendelseTjenesteProvider, hendelseRepository);
    }

    @Test
    public void skal_berike_og_grovsortere_fødselshendelse_som_er_klar() {
        // Arrange
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of(new AktørId(AKTØR_ID_MOR), new AktørId(AKTØR_ID_FAR)));

        InngåendeHendelse inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now());
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        InngåendeHendelse beriketHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(beriketHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.SENDT_TIL_SORTERING);
        PdlFødsel beriketFødsel = JsonMapper.fromJson(beriketHendelse.getPayload(), PdlFødsel.class);
        assertThat(beriketFødsel.getAktørIdForeldre()).containsExactly(AKTØR_ID_MOR, AKTØR_ID_FAR);

        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(SorterHendelserTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_REQUEST_UUID)).isEqualTo(HENDELSE_ID);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo(HENDELSE_ID);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isEqualTo(inngåendeHendelse.getId().toString());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
    }

    @Test
    public void skal_opprette_ny_vurder_sortering_task_for_fødselshendelse_som_ikke_er_klar() {
        // Arrange
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of()); // Foreldre finnes ikke i TPS

        InngåendeHendelse inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now());
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(VurderSorteringTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo(HENDELSE_ID);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isEqualTo(inngåendeHendelse.getId().toString());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now()).toLocalDate());

        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
    }

    @Test
    public void skal_ikke_opprette_ny_vurder_sortering_task_for_fødselshendelse_som_ikke_er_klar_når_hendelsen_er_gammel() {
        // Arrange
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of()); // Foreldre finnes ikke i TPS

        InngåendeHendelse inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now().minusDays(8));
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));

        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    private InngåendeHendelse opprettInngåendeHendelse(LocalDateTime opprettetTid) {
        PdlFødsel.Builder pdlFødsel = PdlFødsel.builder();
        pdlFødsel.medHendelseId(HENDELSE_ID);
        pdlFødsel.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        pdlFødsel.medEndringstype(PdlEndringstype.OPPRETTET);
        pdlFødsel.leggTilPersonident(AKTØR_ID_BARN);
        pdlFødsel.medFødselsdato(LocalDate.now());
        pdlFødsel.medOpprettet(opprettetTid);
        return InngåendeHendelse.builder()
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .payload(JsonMapper.toJson(pdlFødsel.build()))
                .build();
    }
}