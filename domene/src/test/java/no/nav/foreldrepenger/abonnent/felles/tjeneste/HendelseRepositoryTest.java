package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.felles.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.felles.domene.FeedKode;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;

public class HendelseRepositoryTest {
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }
    private static final String HENDELSE_ID = "1000";

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());

    @Test
    public void skal_hente_payload_for_alle_hendelser_med_request_uuid_som_er_sendt_til_sortering() {
        // Arrange
        InngåendeHendelse hendelse1 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "0", "req_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse2 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "1", "req_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse3 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "2", "req_annen_uuid",
                HåndtertStatusType.SENDT_TIL_SORTERING);
        InngåendeHendelse hendelse4 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "3", "req_uuid",
                HåndtertStatusType.MOTTATT);
        InngåendeHendelse hendelse5 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "4", "req_uuid",
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
    public void skal_returnere_hendelse_fra_PDL_som_er_grovsortert() {
        // Arrange
        InngåendeHendelse hendelse1 = InngåendeHendelse.builder()
                .hendelseId(HENDELSE_ID)
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload1")
                .feedKode(FeedKode.PDL)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse2 = InngåendeHendelse.builder() // Feil feed
                .hendelseId(HENDELSE_ID)
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload2")
                .feedKode(FeedKode.TPS)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse3 = InngåendeHendelse.builder() // Feil hendelseId
                .hendelseId(HENDELSE_ID + 1)
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload3")
                .feedKode(FeedKode.PDL)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse4 = InngåendeHendelse.builder() // Feil håndtertStatus
                .hendelseId(HENDELSE_ID)
                .type(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload4")
                .feedKode(FeedKode.PDL)
                .requestUuid("req_uuid")
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        repoRule.getEntityManager().flush();

        // Act
        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(FeedKode.PDL, HENDELSE_ID);

        // Assert
        assertThat(hendelse).isPresent();
        assertThat(hendelse.get().getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(hendelse.get().getFeedKode()).isEqualTo(FeedKode.PDL);
        assertThat(hendelse.get().getHåndtertStatus()).isEqualTo(HåndtertStatusType.GROVSORTERT);
        assertThat(hendelse.get().getPayload()).isEqualTo("payload1");
    }
}
