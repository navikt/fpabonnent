package no.nav.foreldrepenger.abonnent.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;

public class HendelseRepositoryTest {

    private static final Long ID = 1000L;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());

    @Test
    @Ignore
    public void skal_hente_alle_hendelser_som_er_klare_til_grovsortering_og_sortere_på_opprettet_tid() {
        InngåendeHendelse hendelse1 = InngåendeHendelse.builder()
                .sekvensnummer(ID)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now().minusMinutes(2))
                .build();
        InngåendeHendelse hendelse2 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 1)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now().minusMinutes(1))
                .build();
        InngåendeHendelse hendelse3 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 2)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now().plusHours(2))
                .build();
        InngåendeHendelse hendelse4 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 3)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now().minusMinutes(2))
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        List<InngåendeHendelse> hendelser = hendelseRepository.finnHendelserSomErKlareTilGrovsortering();
        assertThat(hendelser).hasSize(2);
        assertThat(hendelser).containsExactly(hendelse4, hendelse2);
    }

    @Test
    public void skal_hente_alle_hendelser_som_ikke_er_håndterte_og_kommer_fra_infotrygd_feed() {
        InngåendeHendelse hendelse1 = InngåendeHendelse.builder()
                .sekvensnummer(ID)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.HÅNDTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse2 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 1)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse3 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 2)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.SENDT_TIL_SORTERING)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse4 = InngåendeHendelse.builder()
                .sekvensnummer(ID + 3)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        repoRule.getEntityManager().flush();

        List<InngåendeHendelse> hendelser = hendelseRepository.finnAlleIkkeSorterteHendelserFraFeed(FeedKode.INFOTRYGD);
        assertThat(hendelser).hasSize(2);
        assertThat(hendelser).containsExactly(hendelse3, hendelse4);
    }

    @Test
    public void skal_hente_payload_for_alle_hendelser_med_request_uuid_som_er_sendt_til_sortering() {
        // Arrange
        InngåendeHendelse hendelse1 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(ID, "req_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse2 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(ID + 1, "req_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse3 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(ID + 2, "req_annen_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse4 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(ID + 3, "req_uuid",
                HåndtertStatusType.MOTTATT);
        InngåendeHendelse hendelse5 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(ID + 4, "req_uuid",
                HåndtertStatusType.HÅNDTERT);

        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        hendelseRepository.lagreInngåendeHendelse(hendelse5);
        repoRule.getEntityManager().flush();

        // Act
        List<InngåendeHendelse> hendelser = hendelseRepository
                .finnHendelserSomErSendtTilSorteringMedRequestUUID("req_uuid");

        // Assert
        assertThat(hendelser).hasSize(2);
        assertThat(hendelser).containsOnly(hendelse1, hendelse2);
    }

    @Test
    public void skal_returnere_hendelse_fra_infotrygd_som_er_grovsortert() {
        InngåendeHendelse hendelse1 = InngåendeHendelse.builder()
                .sekvensnummer(ID)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload1")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse2 = InngåendeHendelse.builder() // Feil feed
                .sekvensnummer(ID)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload2")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse3 = InngåendeHendelse.builder() // Feil sekvensnummer
                .sekvensnummer(ID + 1)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload3")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse4 = InngåendeHendelse.builder() // Feil status
                .sekvensnummer(ID)
                .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                .payload("payload4")
                .feedKode(FeedKode.INFOTRYGD)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        repoRule.getEntityManager().flush();

        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(FeedKode.INFOTRYGD, ID);
        assertThat(hendelse).isPresent();
        assertThat(hendelse.get().getSekvensnummer()).isEqualTo(ID);
        assertThat(hendelse.get().getFeedKode()).isEqualTo(FeedKode.INFOTRYGD);
        assertThat(hendelse.get().getHåndtertStatus()).isEqualTo(HåndtertStatusType.GROVSORTERT);
        assertThat(hendelse.get().getPayload()).isEqualTo("payload1");
    }
}
