package no.nav.foreldrepenger.abonnent.felles.task;

import static no.nav.foreldrepenger.abonnent.felles.tjeneste.AktørIdTjeneste.getAktørIderForSortering;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelserKlient;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
@ProsessTask("hendelser.grovsorter")
public class SorterHendelseTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SorterHendelseTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;
    private HendelserKlient hendelser;

    @Inject
    public SorterHendelseTask(ProsessTaskTjeneste prosessTaskTjeneste,
                              InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                              HendelserKlient hendelser) {
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.hendelser = hendelser;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dataWrapper = new HendelserDataWrapper(prosessTaskData);
        String hendelseId = getHendelseId(dataWrapper);

        var inngåendeHendelse = inngåendeHendelseTjeneste.finnHendelseSomErSendtTilSortering(hendelseId);
        if (inngåendeHendelse.isEmpty()) {
            LOGGER.warn("Fant ikke InngåendeHendelse for HendelseId {} - kan ikke grovsortere", hendelseId);
            return;
        }

        var hendelsePayload = inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse.get());
        var aktørIderForSortering = getAktørIderForSortering(hendelsePayload);
        var filtrertAktørIdList = hendelser.grovsorterAktørIder(aktørIderForSortering);

        if (!hendelseErRelevant(filtrertAktørIdList, hendelsePayload)) {
            LOGGER.info("Ikke-relevant hendelse med hendelseId {} og type {} blir ikke videresendt til FPSAK", hendelsePayload.getHendelseId(),
                hendelsePayload.getHendelseType());
            inngåendeHendelseTjeneste.markerHendelseSomHåndtertOgFjernPayload(inngåendeHendelse.get());
            return;
        }

        opprettSendHendelseTask(dataWrapper, hendelsePayload);
        inngåendeHendelseTjeneste.oppdaterHåndtertStatus(inngåendeHendelse.get(), HåndtertStatusType.GROVSORTERT);
        LOGGER.info("Opprettet SendHendelseTask for hendelse {}", hendelseId);
    }

    private String getHendelseId(HendelserDataWrapper dataWrapper) {
        var hendelseId = dataWrapper.getHendelseId();
        if (hendelseId.isEmpty()) {
            throw AbonnentHendelserFeil.prosesstaskPreconditionManglerProperty(dataWrapper.getProsessTaskData().getTaskType(),
                HendelserDataWrapper.HENDELSE_ID, dataWrapper.getId());
        }
        return hendelseId.get();
    }

    private void opprettSendHendelseTask(HendelserDataWrapper dataWrapper, HendelsePayload hendelsePayload) {
        var nesteSteg = dataWrapper.nesteSteg(TaskType.forProsessTask(SendHendelseTask.class));
        nesteSteg.setHendelseId(hendelsePayload.getHendelseId());
        nesteSteg.setHendelseType(hendelsePayload.getHendelseType());
        prosessTaskTjeneste.lagre(nesteSteg.getProsessTaskData());
    }

    private boolean hendelseErRelevant(List<String> aktørIdList, HendelsePayload hendelsePayload) {
        return !Collections.disjoint(hendelsePayload.getAktørIderForSortering(), aktørIdList);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [prosessTaskRepository=" + prosessTaskTjeneste + ", inngåendeHendelseTjeneste="
            + inngåendeHendelseTjeneste + ", hendelser=" + hendelser + "]";
    }
}
