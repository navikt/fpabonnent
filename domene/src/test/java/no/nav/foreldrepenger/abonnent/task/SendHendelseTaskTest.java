package no.nav.foreldrepenger.abonnent.task;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.codahale.metrics.MetricRegistry;

import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.InfotrygdHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.infotrygd.InfotrygdHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.feed.tps.FødselsmeldingOpprettetHendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
import no.nav.tjenester.person.feed.v2.Meldingstype;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class SendHendelseTaskTest {

    private static final String FØDSELSMELDINGSTYPE = Meldingstype.FOEDSELSMELDINGOPPRETTET.name();
    private static final String ENDRINGS_HENDELSE_TYPE = no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype.INFOTRYGD_ENDRET.getType();
    private static final LocalDate FØDSELSDATO = LocalDate.parse("2018-01-01");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseConsumer mockHendelseConsumer;
    private ProsessTaskData prosessTaskData;
    private SendHendelseTask sendHendelseTask;
    private MetricRegistry metricRegistry;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Before
    public void setup() {
        HendelseTjenesteProvider hendelseTjenesteProvider = mock(HendelseTjenesteProvider.class);
        HendelseTjeneste fødselHendelseTjeneste = new FødselsmeldingOpprettetHendelseTjeneste();
        HendelseTjeneste infotrygdHendelseTjeneste = new InfotrygdHendelseTjeneste();
        when(hendelseTjenesteProvider.finnTjeneste(eq(HendelseType.FØDSELSMELDINGOPPRETTET), anyLong())).thenReturn(fødselHendelseTjeneste);
        when(hendelseTjenesteProvider.finnTjeneste(eq(HendelseType.ENDRET), anyLong())).thenReturn(infotrygdHendelseTjeneste);

        mockHendelseConsumer = mock(HendelseConsumer.class);
        metricRegistry = new MetricRegistry();
        inngåendeHendelseTjeneste = mock(InngåendeHendelseTjeneste.class);
        sendHendelseTask = new SendHendelseTask(mockHendelseConsumer, metricRegistry, inngåendeHendelseTjeneste, hendelseTjenesteProvider);

        prosessTaskData = new ProsessTaskData(SendHendelseTask.TASKNAME);
        prosessTaskData.setSekvens("1");
    }

    @Test
    public void skal_sende_fødselshendelse() {
        // Arrange
        HendelserDataWrapper hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setHendelseRequestUuid("req_uuid");
        hendelse.setHendelseSekvensnummer(1L);
        hendelse.setHendelseType(FØDSELSMELDINGSTYPE);
        hendelse.setFødselsdato(FØDSELSDATO);
        hendelse.setAktørIdBarn(new HashSet<>(singletonList("1")));
        hendelse.setAktørIdMor(new HashSet<>(singletonList("2")));
        hendelse.setAktørIdFar(new HashSet<>(singletonList("3")));

        ArgumentCaptor<FødselHendelsePayload> captor = ArgumentCaptor.forClass(FødselHendelsePayload.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        verify(mockHendelseConsumer, times(1)).sendHendelse(captor.capture());
        verify(inngåendeHendelseTjeneste, times(1)).oppdaterHendelseSomSendtNå(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        for (FødselHendelsePayload payload : captor.getAllValues()) {
            assertThat(payload.getSekvensnummer()).isEqualTo(1L);
            assertThat(payload.getType()).isEqualTo(FØDSELSMELDINGSTYPE);
            assertThat(payload.getAktørIdMor()).isPresent();
            assertThat(payload.getAktørIdMor().get()).contains("2");
            assertThat(payload.getAktørIdFar()).isPresent();
            assertThat(payload.getAktørIdFar().get()).contains("3");
            assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);
        }
        assertThat(metricRegistry.meter(SendHendelseTask.TASKNAME).getCount()).isEqualTo(1);
    }

    @Test
    public void skal_håndtere_fødselshendelse_med_mange_aktørIder() {
        // Arrange
        HendelserDataWrapper hendelse = new HendelserDataWrapper(prosessTaskData);
        hendelse.setHendelseRequestUuid("req_uuid");
        hendelse.setHendelseSekvensnummer(1L);
        hendelse.setHendelseType(FØDSELSMELDINGSTYPE);
        hendelse.setFødselsdato(FØDSELSDATO);
        hendelse.setAktørIdBarn(new HashSet<>(asList("1","2")));
        hendelse.setAktørIdMor(new HashSet<>(asList("3","4","5")));
        hendelse.setAktørIdFar(new HashSet<>(asList("6","7")));

        ArgumentCaptor<FødselHendelsePayload> captor = ArgumentCaptor.forClass(FødselHendelsePayload.class);

        // Act
        sendHendelseTask.doTask(hendelse.getProsessTaskData());

        // Assert
        assertThat(hendelse.getProsessTaskData().getPropertyValue(HendelserDataWrapper.AKTØR_ID_BARN)).isEqualTo("1,2");
        assertThat(hendelse.getProsessTaskData().getPropertyValue(HendelserDataWrapper.AKTØR_ID_MOR)).isEqualTo("3,4,5");
        assertThat(hendelse.getProsessTaskData().getPropertyValue(HendelserDataWrapper.AKTØR_ID_FAR)).isEqualTo("6,7");
        verify(mockHendelseConsumer, times(1)).sendHendelse(captor.capture());
        FødselHendelsePayload payload = captor.getValue();
        assertThat(payload.getSekvensnummer()).isEqualTo(1L);
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDINGSTYPE);
        assertThat(payload.getAktørIdMor()).isPresent();
        assertThat(payload.getAktørIdMor().get()).containsExactly("3","4","5");
        assertThat(payload.getAktørIdFar()).isPresent();
        assertThat(payload.getAktørIdFar().get()).containsExactly("6","7");
        assertThat(payload.getFødselsdato()).isPresent().hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_sende_infotrygdhendelse() {
        // Arrange
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseRequestUuid("req_uuid");
        dataWrapper.setHendelseSekvensnummer(1L);
        dataWrapper.setHendelseType(ENDRINGS_HENDELSE_TYPE);
        dataWrapper.setAktørId("1");

        ArgumentCaptor<InfotrygdHendelsePayload> captor = ArgumentCaptor.forClass(InfotrygdHendelsePayload.class);

        // Act
        sendHendelseTask.doTask(dataWrapper.getProsessTaskData());

        // Assert
        verify(mockHendelseConsumer).sendHendelse(captor.capture());
        verify(inngåendeHendelseTjeneste, times(1)).oppdaterHendelseSomSendtNå(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        for (InfotrygdHendelsePayload payload : captor.getAllValues()) {
            assertThat(payload.getSekvensnummer()).isEqualTo(1L);
            assertThat(payload.getType()).isEqualTo(ENDRINGS_HENDELSE_TYPE);
            assertThat(payload.getAktoerId()).isEqualTo("1");
        }
        assertThat(metricRegistry.meter(SendHendelseTask.TASKNAME).getCount()).isEqualTo(1);
    }

    @Test
    public void skal_kaste_feil_for_ukjent_hendelse() {
        // Arrange
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        dataWrapper.setHendelseRequestUuid("req_uuid");
        dataWrapper.setHendelseSekvensnummer(1L);
        dataWrapper.setHendelseType(null);
        dataWrapper.setAktørId("1");

        // Assert
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-846675");

        // Act
        sendHendelseTask.doTask(dataWrapper.getProsessTaskData());
    }

}
