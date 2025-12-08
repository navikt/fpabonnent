package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ExtendWith(MockitoExtension.class)
class ForvaltningRestTest {

    private static final LocalDate FØDSELSDATO = LocalDate.of(2023,7,4);
    private static final String HENDELSE_ID = UUID.randomUUID().toString();


    @Mock
    private HendelseRepository hendelseRepository;


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
        when(hendelseRepository.finnHendelseFraIdHvisFinnes(eq(HENDELSE_ID), eq(HendelseKilde.PDL))).thenReturn(Optional.of(inngåendeHendelse));

        var forvaltning = new ForvaltningRestTjeneste(hendelseRepository);
        var dtos = (MigreringHendelseDto) (forvaltning.lesHendelser().getEntity());
        var serializedDtos = DefaultJsonMapper.toJson(dtos);
        var deserDtos = DefaultJsonMapper.fromJson(serializedDtos, MigreringHendelseDto.class);

        var resp = forvaltning.sammenlignHendelser(deserDtos);
        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        var deserInngående = forvaltning.lagreHendelser(deserDtos);

        var hendelseCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(1)).lagreInngåendeHendelse(hendelseCaptor.capture());

        var lagretInn = hendelseCaptor.getValue();
        assertThat(lagretInn.getHendelseId()).isEqualTo(inngåendeHendelse.getHendelseId());
        assertThat(lagretInn.getHendelseType()).isEqualTo(inngåendeHendelse.getHendelseType());
        assertThat(lagretInn.getPayload()).isEqualTo(inngåendeHendelse.getPayload());

    }



}
