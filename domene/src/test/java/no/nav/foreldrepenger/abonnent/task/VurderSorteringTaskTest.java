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
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
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
        HendelseTjeneste hendelseTjeneste = new PdlFødselHendelseTjeneste(personTjeneste, hendelseRepository);
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);
        vurderSorteringTask = new VurderSorteringTask(prosessTaskRepository, tpsForsinkelseTjeneste, hendelseTjenesteProvider, hendelseRepository);
    }

    @Test
    public void skal_berike_og_grovsortere_fødselshendelse_som_er_klar() {
        // Arrange
        when(personTjeneste.erRegistrert(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(true);
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
        // Barn og relasjon til foreldre finnes ikke i TPS:
        when(personTjeneste.erRegistrert(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(false);
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of());

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
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), inngåendeHendelse).toLocalDate());

        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
    }

    @Test
    public void skal_ikke_opprette_ny_vurder_sortering_task_for_fødselshendelse_som_ikke_er_klar_når_hendelsen_er_gammel() {
        // Arrange
        // Barn og relasjon til foreldre finnes ikke i TPS:
        when(personTjeneste.erRegistrert(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(false);
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of());

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

    @Test
    public void skal_ikke_grovsortere_fødselshendelse_med_fødselsdato_over_to_år_tilbake_i_tid() {
        // Arrange
        InngåendeHendelse hendelseOpprettet = InngåendeHendelse.builder()
                .hendelseId("A")
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .feedKode(FeedKode.PDL)
                .sendtTidspunkt(LocalDateTime.now())
                .requestUuid("1")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now().minusYears(10), PdlEndringstype.OPPRETTET, "A", null).build()))
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        repoRule.getEntityManager().flush();

        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseOpprettet.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId("A");

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(hendelseOpprettet.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(personTjeneste, times(0)).erRegistrert(any());
        verify(personTjeneste, times(0)).registrerteForeldre(any());
        verify(prosessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void skal_grovsortere_korrigering_da_en_tidligere_sendt_fødselshendelse_hadde_forskjellig_fødselsdato() {
        // Arrange
        when(personTjeneste.erRegistrert(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(true);
        when(personTjeneste.registrerteForeldre(eq(new AktørId(AKTØR_ID_BARN)))).thenReturn(Set.of(new AktørId(AKTØR_ID_MOR), new AktørId(AKTØR_ID_FAR)));

        InngåendeHendelse hendelseOpprettet = InngåendeHendelse.builder()
                .hendelseId("A")
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .feedKode(FeedKode.PDL)
                .sendtTidspunkt(LocalDateTime.now())
                .requestUuid("1")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        InngåendeHendelse hendelseKorrigert1 = InngåendeHendelse.builder()
                .hendelseId("B")
                .type(HendelseType.PDL_FØDSEL_KORRIGERT)
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .feedKode(FeedKode.PDL)
                .sendtTidspunkt(null)
                .requestUuid("2")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
                .tidligereHendelseId("A")
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert1);
        InngåendeHendelse hendelseKorrigert2 = InngåendeHendelse.builder()
                .hendelseId("C")
                .type(HendelseType.PDL_FØDSEL_KORRIGERT)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .feedKode(FeedKode.PDL)
                .requestUuid("3")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now().minusDays(1), PdlEndringstype.KORRIGERT, "C", "B").build()))
                .tidligereHendelseId("B")
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert2);
        repoRule.getEntityManager().flush();

        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert2.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("C");

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        InngåendeHendelse beriketHendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert2.getId());
        assertThat(beriketHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.SENDT_TIL_SORTERING);
        PdlFødsel beriketFødsel = JsonMapper.fromJson(beriketHendelse.getPayload(), PdlFødsel.class);
        assertThat(beriketFødsel.getAktørIdForeldre()).containsExactly(AKTØR_ID_MOR, AKTØR_ID_FAR);

        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(SorterHendelserTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("C");
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
    }

    @Test
    public void skal_ikke_grovsortere_korrigering_da_en_tidligere_sendt_fødselshendelse_har_samme_fødselsdato() {
        // Arrange
        InngåendeHendelse hendelseOpprettet = InngåendeHendelse.builder()
                .hendelseId("A")
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .feedKode(FeedKode.PDL)
                .sendtTidspunkt(LocalDateTime.now())
                .requestUuid("1")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        InngåendeHendelse hendelseKorrigert1 = InngåendeHendelse.builder()
                .hendelseId("B")
                .type(HendelseType.PDL_FØDSEL_KORRIGERT)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .feedKode(FeedKode.PDL)
                .sendtTidspunkt(null)
                .requestUuid("2")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
                .tidligereHendelseId("A")
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert1);
        repoRule.getEntityManager().flush();

        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert1.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert1.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(personTjeneste, times(0)).erRegistrert(any());
        verify(personTjeneste, times(0)).registrerteForeldre(any());
        verify(prosessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    public void skal_utsette_grovsortering_av_hendelser_som_har_en_tidligere_hendelse_som_ikke_er_håndtert_enda() {
        // Arrange
        LocalDateTime håndteresTidspunktA = LocalDateTime.now();
        InngåendeHendelse hendelseOpprettet = InngåendeHendelse.builder()
                .hendelseId("A")
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .feedKode(FeedKode.PDL)
                .requestUuid("1")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
                .håndteresEtterTidspunkt(håndteresTidspunktA)
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        InngåendeHendelse hendelseKorrigert = InngåendeHendelse.builder()
                .hendelseId("B")
                .type(HendelseType.PDL_FØDSEL_KORRIGERT)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .feedKode(FeedKode.PDL)
                .requestUuid("2")
                .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
                .tidligereHendelseId("A")
                .håndteresEtterTidspunkt(håndteresTidspunktA.minusMinutes(2))
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert);
        repoRule.getEntityManager().flush();

        HendelserDataWrapper hendelserDataWrapper = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        InngåendeHendelse hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(hendelse.getHåndteresEtterTidspunkt()).isEqualTo(håndteresTidspunktA.plusMinutes(2));

        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(VurderSorteringTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("B");
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        assertThat(prosessTaskData.getNesteKjøringEtter()).isEqualTo(håndteresTidspunktA.plusMinutes(2));

        verify(personTjeneste, times(0)).erRegistrert(any());
        verify(personTjeneste, times(0)).registrerteForeldre(any());
    }

    private InngåendeHendelse opprettInngåendeHendelse(LocalDateTime opprettetTid) {
        PdlFødsel.Builder pdlFødsel = opprettFødsel(opprettetTid, LocalDate.now(), PdlEndringstype.OPPRETTET, HENDELSE_ID, null);
        return InngåendeHendelse.builder()
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .payload(JsonMapper.toJson(pdlFødsel.build()))
                .build();
    }

    private PdlFødsel.Builder opprettFødsel(LocalDateTime opprettetTid, LocalDate fødselsdato, PdlEndringstype endringstype, String hendelseId, String tidligereHendelseID) {
        PdlFødsel.Builder pdlFødsel = PdlFødsel.builder();
        pdlFødsel.medHendelseId(hendelseId);
        pdlFødsel.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        pdlFødsel.medEndringstype(endringstype);
        pdlFødsel.leggTilPersonident(AKTØR_ID_BARN);
        pdlFødsel.medFødselsdato(fødselsdato);
        pdlFødsel.medOpprettet(opprettetTid);
        pdlFødsel.medTidligereHendelseId(tidligereHendelseID);
        return pdlFødsel;
    }
}