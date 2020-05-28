package no.nav.foreldrepenger.abonnent.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
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
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;

public class PdlLeesahHendelseHåndtererTest {

    private HendelseRepository hendelseRepository;
    private PdlLeesahHendelseHåndterer hendelseHåndterer;

    private static final LocalDateTime OPPRETTET_TID = LocalDateTime.now();
    private static final LocalDate DØDSDATO = LocalDate.now().minusDays(1);

    @Before
    public void before() {
        hendelseRepository = mock(HendelseRepository.class);
        PdlLeesahOversetter oversetter = new PdlLeesahOversetter();

        hendelseHåndterer = new PdlLeesahHendelseHåndterer(hendelseRepository, oversetter, new PdlFeatureToggleTjeneste());
    }

    @Test
    public void skal_oversette_og_lagre_dødshendelse() {
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

        // Act
        hendelseHåndterer.handleMessage("", payload);

        // Assert
        InngåendeHendelse inngåendeHendelse = hendelseCaptor.getValue();
        assertThat(inngåendeHendelse.getPayload()).contains("\"hendelseId\":\"ABC\"", "\"personidenter\":[\"1111111111111\",\"22222222222\"]", "\"master\":\"Freg\"", "\"opplysningstype\":\"DOEDSFALL_V1\"", "\"endringstype\":\"OPPRETTET\"", "\"hendelseType\":{\"kode\":\"PDL_DOED_OPPRETTET\"", "\"kodeverk\":\"HENDELSE_TYPE\"}");
        assertThat(inngåendeHendelse.getHendelseId()).isEqualTo("ABC");
        assertThat(inngåendeHendelse.getHåndtertStatus()).isEqualTo(HåndtertStatusType.MOTTATT);
        assertThat(inngåendeHendelse.getFeedKode()).isEqualTo(FeedKode.PDL);
        assertThat(inngåendeHendelse.getType()).isEqualTo(HendelseType.PDL_DØD_OPPRETTET);
        assertThat(inngåendeHendelse.getHåndteresEtterTidspunkt()).isNotNull();
    }

}