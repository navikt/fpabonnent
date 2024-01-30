package no.nav.foreldrepenger.abonnent.pdl.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.DateUtil;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseKonfig;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.foedsel.Foedsel;
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

class PdlLeesahHendelseHåndtererTest {

    private HendelseRepository hendelseRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForsinkelseTjeneste forsinkelseTjeneste;

    private PdlLeesahHendelseHåndterer hendelseHåndterer;

    private static final LocalDateTime OPPRETTET_TID = LocalDateTime.now();
    private static final LocalDate DØDSDATO = LocalDate.now().minusDays(1);
    private static final LocalDate UTFLYTTINGSDATO = LocalDate.now().minusDays(1);

    @BeforeEach
    void before() {
        hendelseRepository = mock(HendelseRepository.class);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        var forsinkelseKonfig = mock(ForsinkelseKonfig.class);
        when(forsinkelseKonfig.skalForsinkeHendelser()).thenReturn(true);
        forsinkelseTjeneste = new ForsinkelseTjeneste(forsinkelseKonfig, hendelseRepository, new DateUtil());

        hendelseHåndterer = new PdlLeesahHendelseHåndterer(hendelseRepository, new PdlLeesahOversetter(), prosessTaskTjeneste, forsinkelseTjeneste);
    }

    @Test
    void skal_lagre_oversatt_dødshendelse_og_opprette_vurder_sortering_task() {
        // Arrange
        var payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("DOEDSFALL_V1");
        payload.setEndringstype(Endringstype.OPPRETTET);
        var doedsfall = new Doedsfall();
        doedsfall.setDoedsdato(DØDSDATO);
        payload.setDoedsfall(doedsfall);
        var hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        doNothing().when(hendelseRepository).lagreInngåendeHendelse(hendelseCaptor.capture());
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        var inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getPayload()).contains("\"hendelseId\":\"ABC\"", "\"personidenter\":[\"1111111111111\",\"22222222222\"]",
            "\"master\":\"Freg\"", "\"opplysningstype\":\"DOEDSFALL_V1\"", "\"endringstype\":\"OPPRETTET\"",
            "\"hendelseType\":\"PDL_DOED_OPPRETTET\"");
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getHendelseKilde()).isEqualTo(HendelseKilde.PDL);
        assertThat(inngåendeHendelse.getHendelseType()).isEqualTo(HendelseType.PDL_DØD_OPPRETTET);

        var prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(VurderSorteringTask.class));
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("ABC");
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(
            forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(inngåendeHendelse).toLocalDate());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_DØD_OPPRETTET.getKode());
    }

    @Test
    void skal_lagre_oversatt_utflyttingshendelse_og_opprette_vurder_sortering_task() {
        // Arrange
        var payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("UTFLYTTING_FRA_NORGE");
        payload.setEndringstype(Endringstype.OPPRETTET);
        var utflyttingFraNorge = new UtflyttingFraNorge();
        utflyttingFraNorge.setUtflyttingsdato(UTFLYTTINGSDATO);
        payload.setUtflyttingFraNorge(utflyttingFraNorge);
        var hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        doNothing().when(hendelseRepository).lagreInngåendeHendelse(hendelseCaptor.capture());
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        var inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getHendelseKilde()).isEqualTo(HendelseKilde.PDL);
        assertThat(inngåendeHendelse.getHendelseType()).isEqualTo(HendelseType.PDL_UTFLYTTING_OPPRETTET);

        var prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(VurderSorteringTask.class));
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("ABC");
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(
            forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(inngåendeHendelse).toLocalDate());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_UTFLYTTING_OPPRETTET.getKode());
    }

    @Test
    void skal_lagre_annullert_fødselshendelse_og_opprette_vurder_sortering_task() {
        // Arrange
        var payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("FOEDSEL_V1");
        payload.setEndringstype(Endringstype.ANNULLERT);
        var hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        doNothing().when(hendelseRepository).lagreInngåendeHendelse(hendelseCaptor.capture());
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskTjeneste).lagre(taskCaptor.capture());

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        var inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getPayload()).contains("\"hendelseId\":\"ABC\"", "\"personidenter\":[\"1111111111111\",\"22222222222\"]",
            "\"master\":\"Freg\"", "\"opplysningstype\":\"FOEDSEL_V1\"", "\"endringstype\":\"ANNULLERT\"",
            "\"hendelseType\":\"PDL_FOEDSEL_ANNULLERT\"");
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getHendelseKilde()).isEqualTo(HendelseKilde.PDL);
        assertThat(inngåendeHendelse.getHendelseType()).isEqualTo(HendelseType.PDL_FØDSEL_ANNULLERT);

        var prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(VurderSorteringTask.class));
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("ABC");
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(
            forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(inngåendeHendelse).toLocalDate());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_ANNULLERT.getKode());
    }

    @Test
    void skal_ikke_lagre_gammel_fødselshendelse() {
        // Arrange
        var payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("FOEDSEL_V1");
        payload.setEndringstype(Endringstype.ANNULLERT);
        var fødselsdato = new Foedsel();
        fødselsdato.setFoedselsdato(LocalDate.now().minusYears(20));
        payload.setFoedsel(fødselsdato);

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        verify(hendelseRepository, times(0)).lagreInngåendeHendelse(any());
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
        verify(prosessTaskTjeneste, times(0)).lagre(any(ProsessTaskGruppe.class));
    }

}
