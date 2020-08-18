package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.felles.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
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
    public void skal_returnere_hendelse_som_er_sendt_til_sortering() {
        // Arrange
        InngåendeHendelse hendelse = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID, HåndtertStatusType.SENDT_TIL_SORTERING);
        hendelseRepository.lagreInngåendeHendelse(hendelse);
        repoRule.getEntityManager().flush();

        // Act
        Optional<InngåendeHendelse> resultat = hendelseRepository.finnHendelseSomErSendtTilSortering(HENDELSE_ID);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getHendelseId()).isEqualTo(HENDELSE_ID);
    }

    @Test
    public void skal_returnere_hendelse_fra_angitt_kilde() {
        // Arrange
        InngåendeHendelse hendelse1 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "1", HåndtertStatusType.MOTTATT);
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        InngåendeHendelse hendelse2 = HendelseTestDataUtil.lagInngåendeFødselsHendelse(HENDELSE_ID + "2", HåndtertStatusType.MOTTATT);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        repoRule.getEntityManager().flush();

        // Act
        Optional<InngåendeHendelse> resultat = hendelseRepository.finnHendelseFraIdHvisFinnes(HENDELSE_ID + "1", HendelseKilde.PDL);

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getHendelseId()).isEqualTo(HENDELSE_ID + "1");
    }

    @Test
    public void skal_returnere_hendelse_fra_PDL_som_er_grovsortert() {
        // Arrange
        InngåendeHendelse hendelse1 = InngåendeHendelse.builder()
                .hendelseId(HENDELSE_ID)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload1")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse2 = InngåendeHendelse.builder() // Feil kilde
                .hendelseId(HENDELSE_ID)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload2")
                .hendelseKilde(HendelseKilde.TPS)
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse3 = InngåendeHendelse.builder() // Feil hendelseId
                .hendelseId(HENDELSE_ID + 1)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload3")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.GROVSORTERT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        InngåendeHendelse hendelse4 = InngåendeHendelse.builder() // Feil håndtertStatus
                .hendelseId(HENDELSE_ID)
                .hendelseType(HendelseType.PDL_FØDSEL_OPPRETTET)
                .payload("payload4")
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(HåndtertStatusType.MOTTATT)
                .håndteresEtterTidspunkt(LocalDateTime.now())
                .build();
        hendelseRepository.lagreInngåendeHendelse(hendelse1);
        hendelseRepository.lagreInngåendeHendelse(hendelse2);
        hendelseRepository.lagreInngåendeHendelse(hendelse3);
        hendelseRepository.lagreInngåendeHendelse(hendelse4);
        repoRule.getEntityManager().flush();

        // Act
        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(HendelseKilde.PDL, HENDELSE_ID);

        // Assert
        assertThat(hendelse).isPresent();
        assertThat(hendelse.get().getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(hendelse.get().getHendelseKilde()).isEqualTo(HendelseKilde.PDL);
        assertThat(hendelse.get().getHåndtertStatus()).isEqualTo(HåndtertStatusType.GROVSORTERT);
        assertThat(hendelse.get().getPayload()).isEqualTo("payload1");
    }
}
