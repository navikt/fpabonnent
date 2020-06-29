package no.nav.foreldrepenger.abonnent.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.task.VurderSorteringTask;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class PdlLeesahHendelseHåndtererTest {

    private HendelseRepository hendelseRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private TpsForsinkelseTjeneste tpsForsinkelseTjeneste;

    private PdlLeesahHendelseHåndterer hendelseHåndterer;

    private static final LocalDateTime OPPRETTET_TID = LocalDateTime.now();
    private static final LocalDate DØDSDATO = LocalDate.now().minusDays(1);

    @Before
    public void before() {
        hendelseRepository = mock(HendelseRepository.class);
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        tpsForsinkelseTjeneste = new TpsForsinkelseTjeneste();

        hendelseHåndterer = new PdlLeesahHendelseHåndterer(hendelseRepository, new PdlLeesahOversetter(), prosessTaskRepository, tpsForsinkelseTjeneste, new PdlFeatureToggleTjeneste());
    }

    @Test
    public void skal_lagre_oversatt_dødshendelse_og_opprette_vurder_sortering_task() {
        // Arrange
        Personhendelse payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("DOEDSFALL_V1");
        payload.setEndringstype(Endringstype.OPPRETTET);
        Doedsfall doedsfall = new Doedsfall();
        doedsfall.setDoedsdato(DØDSDATO);
        payload.setDoedsfall(doedsfall);
        ArgumentCaptor<InngåendeHendelse> hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        doNothing().when(hendelseRepository).lagreInngåendeHendelse(hendelseCaptor.capture());
        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        InngåendeHendelse inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getPayload()).contains("\"hendelseId\":\"ABC\"", "\"personidenter\":[\"1111111111111\",\"22222222222\"]", "\"master\":\"Freg\"", "\"opplysningstype\":\"DOEDSFALL_V1\"", "\"endringstype\":\"OPPRETTET\"", "\"hendelseType\":{\"kode\":\"PDL_DOED_OPPRETTET\"", "\"kodeverk\":\"HENDELSE_TYPE\"}");
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getFeedKode()).isEqualTo(FeedKode.PDL);
        assertThat(inngåendeHendelse.getType()).isEqualTo(HendelseType.PDL_DØD_OPPRETTET);

        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(VurderSorteringTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isNotNull();
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("ABC");
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(
                tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(OPPRETTET_TID, inngåendeHendelse).toLocalDate());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_DØD_OPPRETTET.getKode());
    }

    @Test
    public void skal_lagre_annullert_fødselshendelse_og_opprette_vurder_sortering_task() {
        // Arrange
        Personhendelse payload = new Personhendelse();
        payload.setHendelseId("ABC");
        payload.setPersonidenter(List.of("1111111111111", "22222222222"));
        payload.setMaster("Freg");
        payload.setOpprettet(OPPRETTET_TID.atZone(ZoneId.systemDefault()).toInstant());
        payload.setOpplysningstype("FOEDSEL_V1");
        payload.setEndringstype(Endringstype.ANNULLERT);
        ArgumentCaptor<InngåendeHendelse> hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        doNothing().when(hendelseRepository).lagreInngåendeHendelse(hendelseCaptor.capture());
        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn("").when(prosessTaskRepository).lagre(taskCaptor.capture());

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        InngåendeHendelse inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getPayload()).contains("\"hendelseId\":\"ABC\"", "\"personidenter\":[\"1111111111111\",\"22222222222\"]", "\"master\":\"Freg\"", "\"opplysningstype\":\"FOEDSEL_V1\"", "\"endringstype\":\"ANNULLERT\"", "\"hendelseType\":{\"kode\":\"PDL_FOEDSEL_ANNULLERT\"", "\"kodeverk\":\"HENDELSE_TYPE\"}");
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getFeedKode()).isEqualTo(FeedKode.PDL);
        assertThat(inngåendeHendelse.getType()).isEqualTo(HendelseType.PDL_FØDSEL_ANNULLERT);

        ProsessTaskData prosessTaskData = taskCaptor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(VurderSorteringTask.TASKNAME);
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.INNGÅENDE_HENDELSE_ID)).isNotNull();
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo("ABC");
        assertThat(prosessTaskData.getNesteKjøringEtter().toLocalDate()).isEqualTo(
                tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(OPPRETTET_TID, inngåendeHendelse).toLocalDate());
        assertThat(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HendelseType.PDL_FØDSEL_ANNULLERT.getKode());
    }

}