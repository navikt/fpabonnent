package no.nav.foreldrepenger.abonnent.felles.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelserKlient;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hendelser.sendHendelse")
public class SendHendelseTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendHendelseTask.class);

    private HendelserKlient hendelser;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;
    private HendelseRepository hendelseRepository;

    @Inject
    public SendHendelseTask(HendelserKlient hendelser,
                            InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                            HendelseRepository hendelseRepository) {
        this.hendelser = hendelser;
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.hendelseRepository = hendelseRepository;
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
        Long inngåendeHendelseId = dataWrapper.getInngåendeHendelseId()
                .orElseThrow(() -> AbonnentHendelserFeil.manglerInngåendeHendelseIdPåProsesstask(dataWrapper.getProsessTaskData().getTaskType(),
                        dataWrapper.getProsessTaskData().getId()));
        var inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId);
        return inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [hendelser=" + hendelser + ", inngåendeHendelseTjeneste=" + inngåendeHendelseTjeneste
                + ", hendelseRepository=" + hendelseRepository + "]";
    }
}
