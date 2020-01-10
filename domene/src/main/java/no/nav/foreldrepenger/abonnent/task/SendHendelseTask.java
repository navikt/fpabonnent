package no.nav.foreldrepenger.abonnent.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
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
        LOGGER.info("Sendt hendelse: [{}] til FPSAK.", hendelsePayload.getSekvensnummer());
    }

    private HendelsePayload getHendelsePayload(HendelserDataWrapper dataWrapper) {
        String type = dataWrapper.getHendelseType()
                .orElseThrow(() -> AbonnentHendelserFeil.FACTORY.ukjentHendelseType(null).toException());
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider
                .finnTjeneste(HendelseType.fraKode(type), dataWrapper.getHendelseSekvensnummer().orElse(null));
        return hendelseTjeneste.payloadFraWrapper(dataWrapper);
    }
}
