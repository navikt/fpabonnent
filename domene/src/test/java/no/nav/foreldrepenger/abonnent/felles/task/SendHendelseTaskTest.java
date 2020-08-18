package no.nav.foreldrepenger.abonnent.felles.task;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.PdlFødselHendelseTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class SendHendelseTaskTest {

    private static final HendelseType HENDELSE_TYPE = HendelseType.PDL_FØDSEL_OPPRETTET;
    private static final LocalDate FØDSELSDATO = LocalDate.parse("2018-01-01");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseConsumer mockHendelseConsumer;
    private ProsessTaskData prosessTaskData;
    private SendHendelseTask sendHendelseTask;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Before
    public void setup() {
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste fødselHendelseTjeneste = new PdlFødselHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(eq(HENDELSE_TYPE), anyString())).thenReturn(fødselHendelseTjeneste);

        mockHendelseConsumer = mock(HendelseConsumer.class);
        inngåendeHendelseTjeneste = mock(InngåendeHendelseTjeneste.class);
        sendHendelseTask = new SendHendelseTask(mockHendelseConsumer, inngåendeHendelseTjeneste, hendelseTjenesteProvider);

        prosessTaskData = new ProsessTaskData(SendHendelseTask.TASKNAME);
        prosessTaskData.setSekvens("1");
    }

    @Test
    public void skal_sende_fødselshendelse() {
        // Arrange
        HendelserDataWrapper hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setHendelseId("1");
        hendelse.setHendelseType(HENDELSE_TYPE.getKode());
        hendelse.setFødselsdato(FØDSELSDATO);
        hendelse.setAktørIdBarn(new HashSet<>(singletonList("1")));
        hendelse.setAktørIdForeldre(Set.of("2", "3"));

        ArgumentCaptor<PdlFødselHendelsePayload> captor = ArgumentCaptor.forClass(PdlFødselHendelsePayload.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        verify(mockHendelseConsumer, times(1)).sendHendelse(captor.capture());
        verify(inngåendeHendelseTjeneste, times(1)).oppdaterHendelseSomSendtNå(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        for (PdlFødselHendelsePayload payload : captor.getAllValues()) {
            assertThat(payload.getHendelseId()).isEqualTo("1");
            assertThat(payload.getHendelseType()).isEqualTo(HENDELSE_TYPE.getKode());
            assertThat(payload.getAktørIdForeldre()).isPresent();
            assertThat(payload.getAktørIdForeldre().get()).containsExactlyInAnyOrder("2", "3");
            assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);
        }
    }

    @Test
    public void skal_håndtere_fødselshendelse_med_mange_aktørIder() {
        // Arrange
        HendelserDataWrapper hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setHendelseId("1");
        hendelse.setHendelseType(HENDELSE_TYPE.getKode());
        hendelse.setFødselsdato(FØDSELSDATO);
        hendelse.setAktørIdBarn(new HashSet<>(asList("1","2")));
        hendelse.setAktørIdForeldre(new HashSet<>(asList("3","4","5","6","7")));

        ArgumentCaptor<PdlFødselHendelsePayload> captor = ArgumentCaptor.forClass(PdlFødselHendelsePayload.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        assertThat(hendelse.getProsessTaskData().getPropertyValue(HendelserDataWrapper.AKTØR_ID_BARN)).isEqualTo("1,2");
        assertThat(hendelse.getProsessTaskData().getPropertyValue(HendelserDataWrapper.AKTØR_ID_FORELDRE)).isEqualTo("3,4,5,6,7");
        verify(mockHendelseConsumer, times(1)).sendHendelse(captor.capture());
        PdlFødselHendelsePayload payload = captor.getValue();
        assertThat(payload.getHendelseId()).isEqualTo("1");
        assertThat(payload.getHendelseType()).isEqualTo(HENDELSE_TYPE.getKode());
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdBarn().get()).containsExactlyInAnyOrder("1", "2");
        assertThat(payload.getAktørIdForeldre()).isPresent();
        assertThat(payload.getAktørIdForeldre().get()).containsExactlyInAnyOrder("3", "4", "5", "6", "7");
        assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_kaste_feil_for_ukjent_hendelse() {
        // Arrange
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseId("1");
        dataWrapper.setHendelseType(null);
        dataWrapper.setAktørId("1");

        // Assert
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-846675");

        // Act
        sendHendelseTask.doTask(dataWrapper.getProsessTaskData());
    }

}
