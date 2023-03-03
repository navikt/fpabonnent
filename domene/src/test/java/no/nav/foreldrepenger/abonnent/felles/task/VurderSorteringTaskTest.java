package no.nav.foreldrepenger.abonnent.felles.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.oppslag.ForeldreTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseKonfig;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.testutilities.FiktiveFnr;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class VurderSorteringTaskTest {

    private static final String FNR_BARN = new FiktiveFnr().nesteBarnFnr();
    private static final String AKTØR_ID_BARN = "1111111111111";
    private static final String AKTØR_ID_MOR = "2222222222222";
    private static final String AKTØR_ID_FAR = "3333333333333";
    private static final String HENDELSE_ID = "1";

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private ForsinkelseTjeneste forsinkelseTjeneste;

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;

    @Mock
    private ForeldreTjeneste foreldreTjeneste;

    private HendelseRepository hendelseRepository;

    private VurderSorteringTask vurderSorteringTask;

    @BeforeEach
    void before(EntityManager em) {
        this.hendelseRepository = new HendelseRepository(em);
        this.hendelseTjenesteHjelper = new HendelseTjenesteHjelper(hendelseRepository);
        var forsinkelseKonfig = mock(ForsinkelseKonfig.class);
        lenient().when(forsinkelseKonfig.skalForsinkeHendelser()).thenReturn(true);
        forsinkelseTjeneste = new ForsinkelseTjeneste(forsinkelseKonfig, hendelseRepository);

        HendelseTjeneste hendelseTjeneste = new PdlFødselHendelseTjeneste(hendelseTjenesteHjelper, foreldreTjeneste);
        var hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        lenient().when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);

        vurderSorteringTask = new VurderSorteringTask(prosessTaskTjeneste, forsinkelseTjeneste, hendelseTjenesteProvider, hendelseRepository);
    }

    @Test
    void skal_berike_og_grovsortere_fødselshendelse_som_er_klar() {
        // Arrange
        when(foreldreTjeneste.hentForeldre(any(PersonIdent.class))).thenReturn(Set.of(new AktørId(AKTØR_ID_MOR), new AktørId(AKTØR_ID_FAR)));

        var inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now());
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        var vurderSorteringTask = ProsessTaskData.forProsessTask(VurderSorteringTask.class);
        var hendelserDataWrapper = new HendelserDataWrapper(vurderSorteringTask);
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        this.vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var beriketHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(beriketHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.SENDT_TIL_SORTERING);
        var beriketFødsel = JsonMapper.fromJson(beriketHendelse.getPayload(), PdlFødsel.class);
        assertThat(beriketFødsel.getAktørIdForeldre()).containsExactlyInAnyOrder(AKTØR_ID_MOR, AKTØR_ID_FAR);

        var sorterHendelseTask = taskCaptor.getValue();
        assertThat(sorterHendelseTask.taskType()).isEqualTo(TaskType.forProsessTask(SorterHendelseTask.class));
        assertThat(sorterHendelseTask.getSekvens()).isEqualTo("2");
        assertThat(sorterHendelseTask.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo(HENDELSE_ID);
        assertThat(sorterHendelseTask.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isEqualTo(inngåendeHendelse.getId().toString());
        assertThat(sorterHendelseTask.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
    }

    @Test
    void skal_opprette_ny_vurder_sortering_task_for_fødselshendelse_som_ikke_er_klar() {
        // Arrange
        // Relasjon til foreldre finnes ikke i PDL:
        when(foreldreTjeneste.hentForeldre(any(PersonIdent.class))).thenReturn(Set.of());

        var inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now());
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var vurderSorteringTask = taskCaptor.getValue();
        assertThat(vurderSorteringTask.taskType()).isEqualTo(TaskType.forProsessTask(VurderSorteringTask.class));
        assertThat(vurderSorteringTask.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo(HENDELSE_ID);
        assertThat(vurderSorteringTask.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isEqualTo(inngåendeHendelse.getId().toString());
        assertThat(vurderSorteringTask.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        assertThat(vurderSorteringTask.getNesteKjøringEtter().toLocalDate()).isEqualTo(
            forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), inngåendeHendelse).toLocalDate());

        var hendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
    }

    @Test
    void skal_ikke_opprette_ny_vurder_sortering_task_for_fødselshendelse_som_ikke_er_klar_når_hendelsen_er_gammel() {
        // Arrange
        // Relasjon til foreldre finnes ikke i PDL:
        when(foreldreTjeneste.hentForeldre(any(PersonIdent.class))).thenReturn(Set.of());

        var inngåendeHendelse = opprettInngåendeHendelse(LocalDateTime.now().minusDays(8));
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(inngåendeHendelse.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId(HENDELSE_ID);

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));

        var hendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelse.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);
    }

    @Test
    void skal_ikke_grovsortere_fødselshendelse_med_fødselsdato_over_to_år_tilbake_i_tid() {
        // Arrange
        var hendelseOpprettet = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(LocalDateTime.now())
            .payload(
                JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now().minusYears(10), PdlEndringstype.OPPRETTET, "A", null).build()))
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseOpprettet);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseOpprettet.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET.getKode());
        hendelserDataWrapper.setHendelseId("A");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseOpprettet.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_grovsortere_korrigering_da_en_tidligere_sendt_fødselshendelse_hadde_forskjellig_fødselsdato() {
        // Arrange
        when(foreldreTjeneste.hentForeldre(any(PersonIdent.class))).thenReturn(Set.of(new AktørId(AKTØR_ID_MOR), new AktørId(AKTØR_ID_FAR)));

        var hendelseOpprettet = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(LocalDateTime.now())
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        var hendelseKorrigert1 = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert1);
        var hendelseKorrigert2 = InngåendeHendelse.builder()
            .hendelseId("C")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now().minusDays(1), PdlEndringstype.KORRIGERT, "C", "B").build()))
            .tidligereHendelseId("B")
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert2);

        var vurderSorteringTask = ProsessTaskData.forProsessTask(VurderSorteringTask.class);
        var hendelserDataWrapper = new HendelserDataWrapper(vurderSorteringTask);
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert2.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("C");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        this.vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var beriketHendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert2.getId());
        assertThat(beriketHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.SENDT_TIL_SORTERING);
        var beriketFødsel = JsonMapper.fromJson(beriketHendelse.getPayload(), PdlFødsel.class);
        assertThat(beriketFødsel.getAktørIdForeldre()).containsExactlyInAnyOrder(AKTØR_ID_MOR, AKTØR_ID_FAR);

        var sorterHendelseTask = taskCaptor.getValue();
        assertThat(sorterHendelseTask.taskType()).isEqualTo(TaskType.forProsessTask(SorterHendelseTask.class));
        assertThat(sorterHendelseTask.getSekvens()).isEqualTo("2");
        assertThat(sorterHendelseTask.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("C");
        assertThat(sorterHendelseTask.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
    }

    @Test
    void skal_ikke_grovsortere_korrigering_da_en_tidligere_sendt_fødselshendelse_har_samme_fødselsdato() {
        // Arrange
        var hendelseOpprettet = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(LocalDateTime.now())
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        var hendelseKorrigert = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_grovsortere_korrigering_der_tidligere_fødselshendelser_ikke_ble_sendt_til_fpsak() {
        // Arrange
        var hendelseOpprettet = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(
                JsonMapper.toJson(opprettFødsel(LocalDateTime.now().minusDays(2), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        var hendelseKorrigert1 = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.HÅNDTERT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now().minusDays(1), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseKorrigert1);
        var hendelseKorrigert2 = InngåendeHendelse.builder()
            .hendelseId("C")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "C", "B").build()))
            .tidligereHendelseId("B")
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert2);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert2.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("C");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert2.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_grovsortere_korrigering_der_tidligere_fødselshendelse_ikke_finnes_i_vårt_system() {
        // Arrange
        var hendelseKorrigert = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_grovsortere_annullering_der_tidligere_dødshendelse_ikke_finnes_i_vårt_system() {
        // Arrange
        var hendelseKorrigert = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_DØD_ANNULLERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .sendtTidspunkt(null)
            .payload(JsonMapper.toJson(opprettDødAnnullert(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.ANNULLERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_DØD_ANNULLERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        lenient().doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.HÅNDTERT);

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_utsette_grovsortering_av_hendelser_som_har_en_tidligere_hendelse_som_ikke_er_håndtert_enda() {
        // Arrange
        LocalDateTime håndteresTidspunktA = LocalDateTime.now();
        var hendelseOpprettet = InngåendeHendelse.builder()
            .hendelseId("A")
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.OPPRETTET, "A", null).build()))
            .håndteresEtterTidspunkt(håndteresTidspunktA)
            .build();
        hendelseRepository.lagreInngåendeHendelse(hendelseOpprettet);
        var hendelseKorrigert = InngåendeHendelse.builder()
            .hendelseId("B")
            .hendelseType(HendelseType.PDL_FØDSEL_KORRIGERT)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .hendelseKilde(HendelseKilde.PDL)
            .payload(JsonMapper.toJson(opprettFødsel(LocalDateTime.now(), LocalDate.now(), PdlEndringstype.KORRIGERT, "B", "A").build()))
            .tidligereHendelseId("A")
            .håndteresEtterTidspunkt(håndteresTidspunktA.minusMinutes(2))
            .build();
        hendelseRepository.lagreFlushInngåendeHendelse(hendelseKorrigert);

        var hendelserDataWrapper = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        hendelserDataWrapper.setInngåendeHendelseId(hendelseKorrigert.getId());
        hendelserDataWrapper.setHendelseType(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        hendelserDataWrapper.setHendelseId("B");

        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        vurderSorteringTask.doTask(hendelserDataWrapper.getProsessTaskData());

        // Assert
        var hendelse = hendelseRepository.finnEksaktHendelse(hendelseKorrigert.getId());
        assertThat(hendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(hendelse.getHåndteresEtterTidspunkt()).isEqualTo(håndteresTidspunktA.plusMinutes(2));

        var vurderSorteringTask = taskCaptor.getValue();
        assertThat(vurderSorteringTask.taskType()).isEqualTo(TaskType.forProsessTask(VurderSorteringTask.class));
        assertThat(vurderSorteringTask.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("B");
        assertThat(vurderSorteringTask.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_KORRIGERT.getKode());
        assertThat(vurderSorteringTask.getNesteKjøringEtter()).isEqualTo(håndteresTidspunktA.plusMinutes(2));

        verify(foreldreTjeneste, times(0)).hentForeldre(any());
    }

    private InngåendeHendelse opprettInngåendeHendelse(LocalDateTime opprettetTid) {
        var pdlFødsel = opprettFødsel(opprettetTid, LocalDate.now(), PdlEndringstype.OPPRETTET, HENDELSE_ID, null);
        return InngåendeHendelse.builder()
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .payload(JsonMapper.toJson(pdlFødsel.build()))
            .build();
    }

    private PdlFødsel.Builder opprettFødsel(LocalDateTime opprettetTid,
                                            LocalDate fødselsdato,
                                            PdlEndringstype endringstype,
                                            String hendelseId,
                                            String tidligereHendelseID) {
        var pdlFødsel = PdlFødsel.builder();
        pdlFødsel.medHendelseId(hendelseId);
        pdlFødsel.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        pdlFødsel.medEndringstype(endringstype);
        pdlFødsel.leggTilPersonident(FNR_BARN);
        pdlFødsel.leggTilPersonident(AKTØR_ID_BARN);
        pdlFødsel.medFødselsdato(fødselsdato);
        pdlFødsel.medOpprettet(opprettetTid);
        pdlFødsel.medTidligereHendelseId(tidligereHendelseID);
        return pdlFødsel;
    }

    private PdlDød.Builder opprettDødAnnullert(LocalDateTime opprettetTid,
                                               LocalDate dødsdato,
                                               PdlEndringstype endringstype,
                                               String hendelseId,
                                               String tidligereHendelseID) {
        var pdlDød = PdlDød.builder();
        pdlDød.medHendelseId(hendelseId);
        pdlDød.medHendelseType(HendelseType.PDL_DØD_ANNULLERT);
        pdlDød.medEndringstype(endringstype);
        pdlDød.leggTilPersonident(AKTØR_ID_BARN);
        pdlDød.medDødsdato(dødsdato);
        pdlDød.medOpprettet(opprettetTid);
        pdlDød.medTidligereHendelseId(tidligereHendelseID);
        return pdlDød;
    }
}
