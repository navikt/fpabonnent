package no.nav.foreldrepenger.abonnent.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.PdlFeatureToggleTjeneste;
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(SendHendelseTask.TASKNAME)
public class SendHendelseTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendHendelseTask.class);

    public static final String TASKNAME = "hendelser.sendHendelse";

    private HendelseConsumer hendelseConsumer;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;
    private HendelseTjenesteProvider hendelseTjenesteProvider;
    private PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste;
    private HendelseRepository hendelseRepository;

    @Inject
    public SendHendelseTask(HendelseConsumer hendelseConsumer,
                            InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                            HendelseTjenesteProvider hendelseTjenesteProvider,
                            PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste,
                            HendelseRepository hendelseRepository) {
        this.hendelseConsumer = hendelseConsumer;
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
        this.pdlFeatureToggleTjeneste = pdlFeatureToggleTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        HendelsePayload hendelsePayload = getHendelsePayload(dataWrapper);

        if (!HendelseType.fraKode(hendelsePayload.getType()).erPdlHendelse() || pdlFeatureToggleTjeneste.skalSendePdl()) {
            hendelseConsumer.sendHendelse(hendelsePayload);
            inngåendeHendelseTjeneste.oppdaterHendelseSomSendtNå(hendelsePayload);
            LOGGER.info("Sendt hendelse: [{}] til FPSAK.", hendelsePayload.getHendelseId());
        } else {
            Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(hendelsePayload.getFeedKode(), hendelsePayload.getHendelseId());
            hendelse.ifPresent(inngåendeHendelse -> hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT));
            LOGGER.info("Sender ikke PDL-hendelse: [{}] til FPSAK da dette er deaktivert.", hendelsePayload.getHendelseId());
        }
    }

    private HendelsePayload getHendelsePayload(HendelserDataWrapper dataWrapper) {
        String type = dataWrapper.getHendelseType()
                .orElseThrow(() -> AbonnentHendelserFeil.FACTORY.ukjentHendelseType(null).toException());
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider
                .finnTjeneste(HendelseType.fraKode(type), dataWrapper.getHendelseId().orElse(null));
        return hendelseTjeneste.payloadFraWrapper(dataWrapper);
    }
}
