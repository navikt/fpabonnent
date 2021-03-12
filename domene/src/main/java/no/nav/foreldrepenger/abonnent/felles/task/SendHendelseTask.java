package no.nav.foreldrepenger.abonnent.felles.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
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
    private HendelseRepository hendelseRepository;

    @Inject
    public SendHendelseTask(HendelseConsumer hendelseConsumer,
                            InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                            HendelseRepository hendelseRepository) {
        this.hendelseConsumer = hendelseConsumer;
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        HendelsePayload hendelsePayload = getHendelsePayload(dataWrapper);

        hendelseConsumer.sendHendelse(hendelsePayload);
        inngåendeHendelseTjeneste.oppdaterHendelseSomSendtNå(hendelsePayload);
        LOGGER.info("Sendt hendelse: [{}] til FPSAK.", hendelsePayload.getHendelseId());
    }

    private HendelsePayload getHendelsePayload(HendelserDataWrapper dataWrapper) {
        Long inngåendeHendelseId = dataWrapper.getInngåendeHendelseId()
                .orElseThrow(() -> AbonnentHendelserFeil.manglerInngåendeHendelseIdPåProsesstask(dataWrapper.getProsessTaskData().getTaskType(), dataWrapper.getProsessTaskData().getId()));
        InngåendeHendelse inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId);
        return inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse);
    }
}
