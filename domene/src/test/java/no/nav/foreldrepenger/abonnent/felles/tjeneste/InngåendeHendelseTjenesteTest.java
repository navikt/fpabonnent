package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.FeedKode;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;

public class InngåendeHendelseTjenesteTest {

    private static final String REQ_UUID = "req_uuid";

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    private HendelseRepository hendelseRepository;

    @Before
    public void setup() {
        hendelseRepository = mock(HendelseRepository.class);
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste hendelseTjeneste = new PdlFødselHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);
        inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository, hendelseTjenesteProvider);
    }

    @Test
    public void skal_returnere_liste_med_FødselHendelsePayload() {
        // Arrange
        List<InngåendeHendelse> inngåendeHendelser = singletonList(lagInngåendeHendelse("1"));

        // Act
        List<HendelsePayload> resultat = inngåendeHendelseTjeneste.getPayloadsForInngåendeHendelser(inngåendeHendelser);

        // Assert
        assertThat(resultat).isNotNull();
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getHendelseId()).isEqualTo("1");
    }
    
    @Test
    public void skal_returnere_tom_liste() {
        // Arrange + Act
        List<HendelsePayload> resultat = inngåendeHendelseTjeneste.getPayloadsForInngåendeHendelser(new ArrayList<>());

        // Assert
        assertThat(resultat).isEmpty();
    }

    //TODO(JEJ): Kommentere inn når payload fjernes igjen:
    @Ignore
    @Test
    public void skal_markere_ikke_relevante_hendelser_som_håndterte_og_fjerne_payload() {
        // Arrange
        InngåendeHendelse hendelse1 = lagInngåendeHendelse("1");
        InngåendeHendelse hendelse2 = lagInngåendeHendelse("2");
        InngåendeHendelse hendelse3 = lagInngåendeHendelse("3");
        InngåendeHendelse hendelse4 = lagInngåendeHendelse("4");
        InngåendeHendelse hendelse5 = lagInngåendeHendelse("5");
        List<InngåendeHendelse> alleHendelser = asList(hendelse1, hendelse2, hendelse3, hendelse4, hendelse5);
        List<InngåendeHendelse> relevanteHendelser = asList(hendelse1, hendelse3, hendelse5);
        List<HendelsePayload> payloadRelevanteHendelser = inngåendeHendelseTjeneste.getPayloadsForInngåendeHendelser(relevanteHendelser);
        Map<String, InngåendeHendelse> inngåendeHendelserMap = alleHendelser.stream()
                .collect(Collectors.toMap(InngåendeHendelse::getHendelseId, ih -> ih));

        // Act
        inngåendeHendelseTjeneste.markerIkkeRelevanteHendelserSomHåndtert(inngåendeHendelserMap, payloadRelevanteHendelser);

        // Assert
        ArgumentCaptor<InngåendeHendelse> argumentCaptor = ArgumentCaptor.forClass(InngåendeHendelse.class);
        verify(hendelseRepository, times(2))
                .oppdaterHåndtertStatus(argumentCaptor.capture(), eq(HåndtertStatusType.HÅNDTERT));
        verify(hendelseRepository, times(2))
                .fjernPayload(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues().stream().map(InngåendeHendelse::getHendelseId).collect(Collectors.toList()))
                .containsExactly("2", "4", "2", "4");
    }

    @Test
    public void skal_markere_hendelse_som_sendt_nå_og_håndtert() {
        // Arrange
        InngåendeHendelse hendelse = lagInngåendeHendelse("1");
        when(hendelseRepository.finnGrovsortertHendelse(any(FeedKode.class), anyString())).thenReturn(Optional.of(hendelse));

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
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload(inngåendeHendelsePayload)
                .feedKode(FeedKode.PDL)
                .requestUuid(REQ_UUID)
                .build();
    }
}
