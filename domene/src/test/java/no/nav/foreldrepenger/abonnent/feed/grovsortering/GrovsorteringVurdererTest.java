package no.nav.foreldrepenger.abonnent.feed.grovsortering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class GrovsorteringVurdererTest {

    private GrovsorteringVurderer grovsorteringVurderer;
    private HendelseRepository hendelseRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Before
    public void before() {
        hendelseRepository = mock(HendelseRepository.class);
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        grovsorteringVurderer = new GrovsorteringVurderer(hendelseRepository, prosessTaskRepository);
    }

    @Test
    public void skal_opprette_en_prosesstask_pr_uuid() {
        // Arrange
        List<InngåendeHendelse> hendelser = new ArrayList<>();
        hendelser.add(opprettInngåendeHendelse(1L, "1"));
        hendelser.add(opprettInngåendeHendelse(2L, "2"));
        hendelser.add(opprettInngåendeHendelse(3L, "2"));
        hendelser.add(opprettInngåendeHendelse(4L, "3"));
        hendelser.add(opprettInngåendeHendelse(5L, "3"));
        hendelser.add(opprettInngåendeHendelse(6L, "4"));
        when(hendelseRepository.finnHendelserSomErKlareTilGrovsortering()).thenReturn(hendelser);

        // Act
        grovsorteringVurderer.vurderGrovsortering();

        // Assert
        verify(hendelseRepository, times(6)).oppdaterHåndtertStatus(any(InngåendeHendelse.class), eq(HåndtertStatusType.SENDT_TIL_SORTERING));
        verify(prosessTaskRepository, times(4)).lagre(any(ProsessTaskData.class));
    }

    private InngåendeHendelse opprettInngåendeHendelse(Long sekvensnr, String uuid) {
        return InngåendeHendelse.builder()
                .sekvensnummer(sekvensnr)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid(uuid)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now().minusHours(2))
                .build();
    }
}