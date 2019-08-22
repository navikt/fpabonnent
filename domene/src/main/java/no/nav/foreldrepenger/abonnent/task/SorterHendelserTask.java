package no.nav.foreldrepenger.abonnent.task;

import static no.nav.foreldrepenger.abonnent.feed.domain.AktørIdTjeneste.getAktørIderForSortering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseConsumer;
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(SorterHendelserTask.TASKNAME)
public class SorterHendelserTask implements ProsessTaskHandler {

    public static final String TASKNAME = "hendelser.grovsorter";
    public static final String METRIC_FRA_TIL_NAME = "abonnent.task.fra." + TASKNAME + ".til." + SendHendelseTask.TASKNAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(SorterHendelserTask.class);

    private ProsessTaskRepository prosessTaskRepository;
    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;
    private HendelseConsumer hendelseConsumer;
    private MetricRegistry metricRegistry;
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    @Inject
    public SorterHendelserTask(ProsessTaskRepository prosessTaskRepository,
                               InngåendeHendelseTjeneste inngåendeHendelseTjeneste,
                               HendelseConsumer hendelseConsumer,
                               MetricRegistry metricRegistry,
                               HendelseTjenesteProvider hendelseTjenesteProvider) {
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.hendelseConsumer = hendelseConsumer;
        this.metricRegistry = metricRegistry;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
    }

    @Timed
    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        precondition(dataWrapper);
        String requestUUID = dataWrapper.getHendelseRequestUuid();

        List<InngåendeHendelse> inngåendeHendelserListe = inngåendeHendelseTjeneste.hentHendelserSomErSendtTilSorteringMedUUID(requestUUID);
        List<HendelsePayload> payloadListe = inngåendeHendelseTjeneste.getPayloadsForInngåendeHendelser(inngåendeHendelserListe);
        List<String> aktørIderForSortering = getAktørIderForSortering(payloadListe);
        List<String> filtrertAktørIdList = hendelseConsumer.grovsorterAktørIder(aktørIderForSortering);
        List<HendelsePayload> relevanteHendelser = filtrerUtRelevanteHendelser(payloadListe, filtrertAktørIdList);

        Map<Long, InngåendeHendelse> inngåendeHendelserMap = inngåendeHendelserListe.stream()
                .collect(Collectors.toMap(InngåendeHendelse::getSekvensnummer, ih -> ih));
        inngåendeHendelseTjeneste.markerIkkeRelevanteHendelserSomHåndtert(inngåendeHendelserMap, relevanteHendelser);

        if (!relevanteHendelser.isEmpty()) {
            for (HendelsePayload payload : relevanteHendelser) {
                HendelseTjeneste<HendelsePayload> hendelseTjeneste = hendelseTjenesteProvider.finnTjeneste(
                        new HendelseType(payload.getType()), payload.getSekvensnummer());

                if (payload.erAtomisk() || hendelseTjeneste.ikkeAtomiskHendelseSkalSendes(payload)) {
                    HendelserDataWrapper nesteSteg = dataWrapper.nesteSteg(SendHendelseTask.TASKNAME);
                    nesteSteg.setHendelseSekvensnummer(payload.getSekvensnummer());
                    nesteSteg.setHendelseType(payload.getType());
                    hendelseTjeneste.populerDatawrapper(payload, nesteSteg);

                    prosessTaskRepository.lagre(nesteSteg.getProsessTaskData());
                    metricRegistry.meter(METRIC_FRA_TIL_NAME).mark();
                    inngåendeHendelseTjeneste.oppdaterHåndtertStatus(inngåendeHendelserMap.get(payload.getSekvensnummer()), HåndtertStatusType.GROVSORTERT);
                } else {
                    inngåendeHendelseTjeneste.oppdaterHåndtertStatus(inngåendeHendelserMap.get(payload.getSekvensnummer()), HåndtertStatusType.HÅNDTERT);
                }
            }

            LOGGER.info("Opprettet SendHendelseTask for hendelser med request UUID: {}", requestUUID); // NOSONAR
        }
    }

    private void precondition(HendelserDataWrapper dataWrapper) {
        if (dataWrapper.getHendelseRequestUuid() == null) {
            throw AbonnentHendelserFeil.FACTORY.prosesstaskPreconditionManglerProperty(TASKNAME, HendelserDataWrapper.HENDELSE_REQUEST_UUID, dataWrapper.getId()).toException();
        }
    }

    private List<HendelsePayload> filtrerUtRelevanteHendelser(List<HendelsePayload> payloadList, List<String> aktørIdList) {
        if (payloadList.isEmpty()) {
            return Collections.emptyList();
        }

        List<HendelsePayload> listPayload = new ArrayList<>();

        for (HendelsePayload hendelsePayload : payloadList) {
            if (finnRelevanteHendelser(aktørIdList, hendelsePayload)) {
                listPayload.add(hendelsePayload);
            } else {
                LOGGER.info("Ikke-relevant hendelse med sekvensummer {} og type {} blir ikke videresendt til FPSAK",
                        hendelsePayload.getSekvensnummer(), hendelsePayload.getType());
            }
        }
        return listPayload;
    }

    private boolean finnRelevanteHendelser(List<String> aktørIdList, HendelsePayload hendelsePayload) {
        return !Collections.disjoint(hendelsePayload.getAktørIderForSortering(), aktørIdList);
    }

}
