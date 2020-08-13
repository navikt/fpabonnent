package no.nav.foreldrepenger.abonnent.felles.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
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
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    @Inject
    public SendHendelseTask(HendelseConsumer hendelseConsumer,
                            InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                            HendelseTjenesteProvider hendelseTjenesteProvider) {
        this.hendelseConsumer = hendelseConsumer;
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
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
        String type = dataWrapper.getHendelseType()
                .orElseThrow(() -> AbonnentHendelserFeil.FACTORY.ukjentHendelseType(null).toException());
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider
                .finnTjeneste(HendelseType.fraKode(type), dataWrapper.getHendelseId().orElse(null));
        return hendelseTjeneste.payloadFraWrapper(dataWrapper);
    }
}
