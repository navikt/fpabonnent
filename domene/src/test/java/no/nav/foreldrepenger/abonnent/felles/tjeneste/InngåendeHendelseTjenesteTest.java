package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;

public class InngåendeHendelseTjenesteTest {

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    private HendelseRepository hendelseRepository;

    @BeforeEach
    public void setup() {
        hendelseRepository = mock(HendelseRepository.class);
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste hendelseTjeneste = new PdlFødselHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);
        inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository, hendelseTjenesteProvider);
    }

    @Test
    public void skal_opprette_PdlFødselHendelsePayload() {
        // Arrange
        InngåendeHendelse inngåendeHendelse = lagInngåendeHendelse("1");

        // Act
        HendelsePayload resultat = inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat.getHendelseId()).isEqualTo("1");
        assertThat(resultat).isInstanceOf(PdlFødselHendelsePayload.class);
    }
    
    @Test
    public void skal_markere_hendelse_som_håndtert_og_fjerne_payload() {
        // Arrange
        InngåendeHendelse hendelse = lagInngåendeHendelse("1");

        // Act
        inngåendeHendelseTjeneste.markerHendelseSomHåndtertOgFjernPayload(hendelse);

        // Assert
        ArgumentCaptor<InngåendeHendelse> argumentCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(1))
                .oppdaterHåndtertStatus(argumentCaptor.capture(), eq(HåndtertStatusType.HÅNDTERT));
        verify(hendelseRepository, times(1))
                .fjernPayload(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getHendelseId()).isEqualTo("1");
    }

    @Test
    public void skal_markere_hendelse_som_sendt_nå_og_håndtert() {
        // Arrange
        InngåendeHendelse hendelse = lagInngåendeHendelse("1");
        when(hendelseRepository.finnGrovsortertHendelse(any(HendelseKilde.class), anyString())).thenReturn(Optional.of(hendelse));

        // Act
        var payload = new PdlFødselHendelsePayload.Builder().hendelseId("1").build();
        inngåendeHendelseTjeneste.oppdaterHendelseSomSendtNå(payload);

        // Assert
        verify(hendelseRepository).markerHendelseSomSendtNå(eq(hendelse));
        verify(hendelseRepository).oppdaterHåndtertStatus(eq(hendelse), eq(HåndtertStatusType.HÅNDTERT));
    }

    private InngåendeHendelse lagInngåendeHendelse(String hendelseId) {
        PdlFødsel.Builder fødselsmelding = new PdlFødsel.Builder();
        fødselsmelding.medHendelseId(hendelseId);
        fødselsmelding.medHendelseType(HendelseType.PDL_FØDSEL_OPPRETTET);
        fødselsmelding.medEndringstype(PdlEndringstype.OPPRETTET);
        fødselsmelding.leggTilPersonident("1111111111111");

        String inngåendeHendelsePayload = JsonMapper.toJson(fødselsmelding.build());

        return new InngåendeHendelse.Builder()
                .hendelseId(hendelseId)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload(inngåendeHendelsePayload)
                .hendelseKilde(HendelseKilde.PDL)
                .build();
    }
}
