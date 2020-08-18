package no.nav.foreldrepenger.abonnent.felles.task;

import static no.nav.foreldrepenger.abonnent.felles.tjeneste.AktørIdTjeneste.getAktørIderForSortering;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.fpsak.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(SorterHendelserTask.TASKNAME)
public class SorterHendelserTask implements ProsessTaskHandler {

    public static final String TASKNAME = "hendelser.grovsorter";

    private static final Logger LOGGER = LoggerFactory.getLogger(SorterHendelserTask.class);

    private ProsessTaskRepository prosessTaskRepository;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;
    private HendelseConsumer hendelseConsumer;
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    @Inject
    public SorterHendelserTask(ProsessTaskRepository prosessTaskRepository,
                               InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                               HendelseConsumer hendelseConsumer,
                               HendelseTjenesteProvider hendelseTjenesteProvider) {
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.hendelseConsumer = hendelseConsumer;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        String hendelseId = getHendelseId(dataWrapper);

        Optional<InngåendeHendelse> inngåendeHendelse = inngåendeHendelseTjeneste.finnHendelseSomErSendtTilSortering(hendelseId);
        if (inngåendeHendelse.isEmpty()) {
            LOGGER.warn("Fant ikke InngåendeHendelse for HendelseId {} - kan ikke grovsortere", hendelseId);
            return;
        }

        HendelsePayload hendelsePayload = inngåendeHendelseTjeneste.hentUtPayloadFraInngåendeHendelse(inngåendeHendelse.get());
        List<String> aktørIderForSortering = getAktørIderForSortering(hendelsePayload);
        List<String> filtrertAktørIdList = hendelseConsumer.grovsorterAktørIder(aktørIderForSortering);

        if (!hendelseErRelevant(filtrertAktørIdList, hendelsePayload)) {
            LOGGER.info("Ikke-relevant hendelse med hendelseId {} og type {} blir ikke videresendt til FPSAK",
                    hendelsePayload.getHendelseId(), hendelsePayload.getHendelseType());
            inngåendeHendelseTjeneste.markerHendelseSomHåndtertOgFjernPayload(inngåendeHendelse.get());
            return;
        }

        opprettSendHendelseTask(dataWrapper, hendelsePayload);
        inngåendeHendelseTjeneste.oppdaterHåndtertStatus(inngåendeHendelse.get(), HåndtertStatusType.GROVSORTERT);
        LOGGER.info("Opprettet SendHendelseTask for hendelse {}", hendelseId);
    }

    private String getHendelseId(HendelserDataWrapper dataWrapper) {
        if (dataWrapper.getHendelseId().isEmpty()) {
            throw AbonnentHendelserFeil.FACTORY.prosesstaskPreconditionManglerProperty(TASKNAME, HendelserDataWrapper.HENDELSE_ID, dataWrapper.getId()).toException();
        } else {
            return dataWrapper.getHendelseId().get();
        }
    }

    private void opprettSendHendelseTask(HendelserDataWrapper dataWrapper, HendelsePayload hendelsePayload) {
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider.finnTjeneste(
                HendelseType.fraKodeDefaultUdefinert(hendelsePayload.getHendelseType()), hendelsePayload.getHendelseId());

        HendelserDataWrapper nesteSteg = dataWrapper.nesteSteg(SendHendelseTask.TASKNAME);
        nesteSteg.setHendelseId(hendelsePayload.getHendelseId());
        nesteSteg.setHendelseType(hendelsePayload.getHendelseType());
        nesteSteg.setEndringstype(hendelsePayload.getEndringstype());
        hendelseTjeneste.populerDatawrapper(hendelsePayload, nesteSteg);
        prosessTaskRepository.lagre(nesteSteg.getProsessTaskData());
    }

    private boolean hendelseErRelevant(List<String> aktørIdList, HendelsePayload hendelsePayload) {
        return !Collections.disjoint(hendelsePayload.getAktørIderForSortering(), aktørIdList);
    }

}
