package no.nav.foreldrepenger.abonnent.felles.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelserKlient;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask("hendelser.sendHendelse")
public class SendHendelseTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendHendelseTask.class);

    private final HendelserKlient hendelser;
    private final InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    @Inject
    public SendHendelseTask(HendelserKlient hendelser, InngåendeHendelseTjeneste inngåendeHendelseTjeneste) {
        this.hendelser = hendelser;
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dataWrapper = new HendelserDataWrapper(prosessTaskData);
        var hendelsePayload = getHendelsePayload(dataWrapper);

        hendelser.sendHendelse(hendelsePayload);
        inngåendeHendelseTjeneste.oppdaterHendelseSomSendtNå(hendelsePayload);
        LOGGER.info("Sendt hendelse: [{}] til FPSAK.", hendelsePayload.getHendelseId());
    }

    private HendelsePayload getHendelsePayload(HendelserDataWrapper dataWrapper) {
        var inngåendeHendelseId = dataWrapper.getHendelseId()
            .orElseThrow(() -> AbonnentHendelserFeil.manglerInngåendeHendelseIdPåProsesstask(dataWrapper.getProsessTaskData().getTaskType(),
                dataWrapper.getProsessTaskData().getId()));
        var inngåendeHendelse = inngåendeHendelseTjeneste.finnHendelse(inngåendeHendelseId, dataWrapper.getHendelseKilde().orElseThrow());
        return inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse);
    }
}
