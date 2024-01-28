package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
public class ForvaltningRestTest {

    private static final HendelseType HENDELSE_TYPE = HendelseType.PDL_FØDSEL_OPPRETTET;
    private static final LocalDate FØDSELSDATO = LocalDate.of(2023,7,4);
    private static final String HENDELSE_ID = "1";


    @Mock
    private HendelseRepository hendelseRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;


    @Test
    void roundtrip_hendelseDto() {
        // Arrange
        var fødselBuilder = PdlFødsel.builder()
            .medFødselsdato(FØDSELSDATO)
            .medHendelseId(HENDELSE_ID)
            .medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .medEndringstype(PdlEndringstype.OPPRETTET)
            .leggTilPersonident("1111111111111")
            .leggTilPersonident("2222222222222")
            .leggTilPersonident("77777777777");
        var fødsel = ((PdlFødsel.Builder) fødselBuilder).build();
        fødsel.setAktørIdForeldre(Set.of("3333333333333", "4444444444444", "5555555555555", "6666666666666"));
        var inngåendeHendelse = InngåendeHendelse.builder()
            .hendelseId(HENDELSE_ID)
            .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .payload(DefaultJsonMapper.toJson(fødsel))
            .build();
        when(hendelseRepository.hentAlleInngåendeHendelser()).thenReturn(List.of(inngåendeHendelse));

        var forvaltning = new ForvaltningRestTjeneste(prosessTaskTjeneste, hendelseRepository);
        var dtos = (MigreringHendelseDto) (forvaltning.lesHendelser().getEntity());
        var serializedDtos = DefaultJsonMapper.toJson(dtos);
        var deserDtos = DefaultJsonMapper.fromJson(serializedDtos, MigreringHendelseDto.class);
        var deserInngående = forvaltning.lagreHendelser(deserDtos);

        var hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(1)).lagreInngåendeHendelse(hendelseCaptor.capture());

        var lagretInn = hendelseCaptor.getValue();
        assertThat(lagretInn.getHendelseId()).isEqualTo(inngåendeHendelse.getHendelseId());
        assertThat(lagretInn.getHendelseType()).isEqualTo(inngåendeHendelse.getHendelseType());
        assertThat(lagretInn.getPayload()).isEqualTo(inngåendeHendelse.getPayload());

    }


    @Test
    void roundtrip_prosesstaskDto() {
        // Arrange
        var nesteKjøring = LocalDateTime.now().plusHours(1);
        var data1 = ProsessTaskData.forProsessTask(VurderSorteringTask.class);
        data1.setStatus(ProsessTaskStatus.KLAR);
        data1.setProperty(HendelserDataWrapper.HENDELSE_ID, HENDELSE_ID);
        data1.setProperty(HendelserDataWrapper.HENDELSE_TYPE, HENDELSE_TYPE.getKode());
        data1.setNesteKjøringEtter(nesteKjøring);
        data1.setSekvens("1");
        when(prosessTaskTjeneste.finnAlle(ProsessTaskStatus.KLAR)).thenReturn(List.of(data1));

        var forvaltning = new ForvaltningRestTjeneste(prosessTaskTjeneste, hendelseRepository);
        var dtos = (MigreringProsesstaskDto) (forvaltning.lesTasks().getEntity());
        var serializedDtos = DefaultJsonMapper.toJson(dtos);
        var deserDtos = DefaultJsonMapper.fromJson(serializedDtos, MigreringProsesstaskDto.class);
        var deserInngående = forvaltning.lagreTasks(deserDtos);

        var hendelseCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste, times(1)).lagre(hendelseCaptor.capture());

        var lagretInn = hendelseCaptor.getValue();
        assertThat(lagretInn.getNesteKjøringEtter()).isEqualTo(nesteKjøring);
        assertThat(lagretInn.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).isEqualTo(HENDELSE_ID);
        assertThat(lagretInn.getPropertyValue(HendelserDataWrapper.HENDELSE_TYPE)).isEqualTo(HENDELSE_TYPE.getKode());

    }

}
