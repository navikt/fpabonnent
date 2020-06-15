package no.nav.foreldrepenger.abonnent.tjenester;

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

import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.tps.FødselsmeldingOpprettetHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.foedselsmelding.FoedselsmeldingOpprettet;

public class InngåendeHendelseTjenesteTest {

    private static final String REQ_UUID = "req_uuid";

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    private HendelseRepository hendelseRepository;

    @Before
    public void setup() {
        hendelseRepository = mock(HendelseRepository.class);
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste hendelseTjeneste = new FødselsmeldingOpprettetHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(any(HendelseType.class), anyString())).thenReturn(hendelseTjeneste);
        inngåendeHendelseTjeneste = new InngåendeHendelseTjeneste(hendelseRepository, hendelseTjenesteProvider);
    }

    @Test
    public void skal_returnere_liste_med_FødselHendelsePayload() {
        // Arrange
        List<InngåendeHendelse> inngåendeHendelser = singletonList(lagInngåendeHendelse(1L));

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
        InngåendeHendelse hendelse1 = lagInngåendeHendelse(1L);
        InngåendeHendelse hendelse2 = lagInngåendeHendelse(2L);
        InngåendeHendelse hendelse3 = lagInngåendeHendelse(3L);
        InngåendeHendelse hendelse4 = lagInngåendeHendelse(4L);
        InngåendeHendelse hendelse5 = lagInngåendeHendelse(5L);
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
        InngåendeHendelse hendelse = lagInngåendeHendelse(1L);
        when(hendelseRepository.finnGrovsortertHendelse(any(FeedKode.class), anyString())).thenReturn(Optional.of(hendelse));

        // Act
        var payload = new FødselHendelsePayload.Builder().hendelseId("1").build();
        inngåendeHendelseTjeneste.oppdaterHendelseSomSendtNå(payload);

        // Assert
        verify(hendelseRepository).markerHendelseSomSendtNå(eq(hendelse));
        verify(hendelseRepository).oppdaterHåndtertStatus(eq(hendelse), eq(HåndtertStatusType.HÅNDTERT));
    }

    private InngåendeHendelse lagInngåendeHendelse(Long sekvensnummer) {
        FoedselsmeldingOpprettet foedselsmelding = new FoedselsmeldingOpprettet();
        String inngåendeHendelsePayload = JsonMapper.toJson(FeedEntry.builder().sequence(sekvensnummer).content(foedselsmelding).build());

        return new InngåendeHendelse.Builder()
                .hendelseId("" + sekvensnummer)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload(inngåendeHendelsePayload)
                .feedKode(FeedKode.TPS)
                .requestUuid(REQ_UUID)
                .build();
    }
}
