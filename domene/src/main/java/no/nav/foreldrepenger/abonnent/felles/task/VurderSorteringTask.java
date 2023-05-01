package no.nav.foreldrepenger.abonnent.felles.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseEndringType;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.DateUtil;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@Dependent
@ProsessTask("hendelser.vurderSortering")
public class VurderSorteringTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VurderSorteringTask.class);

    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final ForsinkelseTjeneste forsinkelseTjeneste;
    private final HendelseTjenesteProvider hendelseTjenesteProvider;
    private final HendelseRepository hendelseRepository;

    @Inject
    public VurderSorteringTask(ProsessTaskTjeneste prosessTaskTjeneste,
                               ForsinkelseTjeneste forsinkelseTjeneste,
                               HendelseTjenesteProvider hendelseTjenesteProvider,
                               HendelseRepository hendelseRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.forsinkelseTjeneste = forsinkelseTjeneste;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dataWrapper = new HendelserDataWrapper(prosessTaskData);
        var hendelseId = dataWrapper.getInngåendeHendelseId()
            .orElseThrow(() -> new IllegalStateException("Prosesstask " + prosessTaskData.getId() + " peker ikke på en gyldig inngående hendelse"));
        var inngåendeHendelse = hendelseRepository.finnEksaktHendelse(hendelseId);

        // Sjekk om blitt håndtert utenfor task
        if (HåndtertStatusType.HÅNDTERT.equals(inngåendeHendelse.getHåndtertStatus())) {
            // Vurder å fjerne payload i dette tilfellet (skal bare skje for oppr->annullert)
            return;
        }

        var hendelseType = dataWrapper.getHendelseType().orElseThrow(AbonnentHendelserFeil::ukjentHendelseType);
        var hendelseTjeneste = getHendelseTjeneste(dataWrapper, hendelseType);
        var hendelsePayload = hendelseTjeneste.payloadFraJsonString(inngåendeHendelse.getPayload());

        if (hendelseTjeneste.vurderOmHendelseKanForkastes(hendelsePayload)) {
            // Går på kjeding og sene korrigeringer (original kan ha blitt søppeltømt)
            ferdigstillHendelseUtenVidereHåndtering(inngåendeHendelse, false);
            return;
        }

        var tidligereHendelseBehandles = enTidligereHendelseSkalBehandles(inngåendeHendelse);
        if (tidligereHendelseBehandles.isPresent()) {
            opprettVurderSorteringTask(hendelsePayload, inngåendeHendelse);
            return;
        }

        if (overlatVurderingTilSenereKjedetHendelse(inngåendeHendelse)) {
            return;
        }

        var klarForSorteringResultat = hendelseTjeneste.vurderOmKlarForSortering(hendelsePayload);
        if (klarForSorteringResultat.hendelseKlarForSortering()) {
            hendelseTjeneste.berikHendelseHvisNødvendig(inngåendeHendelse, klarForSorteringResultat);
            opprettSorteringTask(hendelsePayload.getHendelseId(), inngåendeHendelse, dataWrapper);
        } else if (klarForSorteringResultat.skalPrøveIgjen() && hendelsenErUnderEnUkeGammel(hendelsePayload.getHendelseOpprettetTid())) {
            opprettVurderSorteringTask(hendelsePayload, inngåendeHendelse);
        } else {
            hendelseTjeneste.loggFeiletHendelse(hendelsePayload);
            ferdigstillHendelseUtenVidereHåndtering(inngåendeHendelse, false);
        }
    }

    private Optional<LocalDateTime> enTidligereHendelseSkalBehandles(InngåendeHendelse inngåendeHendelse) {
        // Scenario som kanskje kan oppstå hvis hendelsene kommer samtidig på Kafka, og blir plukket av forskjellige noder samtidig
        return Optional.ofNullable(inngåendeHendelse.getTidligereHendelseId())
            .flatMap(thid ->  hendelseRepository.finnHendelseFraIdHvisFinnes(thid, inngåendeHendelse.getHendelseKilde()))
            .filter(th -> !HåndtertStatusType.HÅNDTERT.equals(th.getHåndtertStatus()))
            .map(InngåendeHendelse::getHåndteresEtterTidspunkt);
    }

    private boolean overlatVurderingTilSenereKjedetHendelse(InngåendeHendelse inngåendeHendelse) {
        // Liste av hendelser som peker tilbake til aktuell hendelse
        List<InngåendeHendelse> senereHendelser = new ArrayList<>();
        var senereHendelseEnnId = inngåendeHendelse.getHendelseId();
        while (senereHendelseEnnId != null) {
            var senereHendelse = hendelseRepository.finnSenereKjedetHendelseHvisStatusMottatt(senereHendelseEnnId);
            senereHendelse.ifPresent(senereHendelser::add);
            senereHendelseEnnId = senereHendelse.map(InngåendeHendelse::getHendelseId).orElse(null);
        }
        var senereAnnullert = senereHendelser.stream()
            .anyMatch(h -> HendelseEndringType.ANNULLERT.equals(h.getHendelseType().getEndringType()));
        if (HendelseEndringType.OPPRETTET.equals(inngåendeHendelse.getHendelseType().getEndringType()) && senereAnnullert) {
            // Har en hel kjede opprettet -> annullert. Kan se bort fra hele kjeden.
            LOGGER.info("Hendelse {} med type {} har kjede opprettet fram til annullert", inngåendeHendelse.getHendelseId(),
                inngåendeHendelse.getHendelseType().getKode());
            senereHendelser.forEach(h -> {
                ferdigstillHendelseUtenVidereHåndtering(h, false);
                hendelseRepository.lagreInngåendeHendelse(h);
            });
            ferdigstillHendelseUtenVidereHåndtering(inngåendeHendelse, false);
        } else if (!senereHendelser.isEmpty()) {
            LOGGER.info("Hendelse {} med type {} har en senere kjedet hendelse", inngåendeHendelse.getHendelseId(),
                inngåendeHendelse.getHendelseType().getKode());
            ferdigstillHendelseUtenVidereHåndtering(inngåendeHendelse, false);
        }
        return !senereHendelser.isEmpty();
    }

    private void opprettSorteringTask(String hendelseId, InngåendeHendelse inngåendeHendelse, HendelserDataWrapper dataWrapper) {
        HendelserDataWrapper grovsorteringTask = dataWrapper.nesteSteg(TaskType.forProsessTask(SorterHendelseTask.class));
        grovsorteringTask.setHendelseId(hendelseId);
        grovsorteringTask.setInngåendeHendelseId(inngåendeHendelse.getId());
        grovsorteringTask.setHendelseType(inngåendeHendelse.getHendelseType().getKode());
        prosessTaskTjeneste.lagre(grovsorteringTask.getProsessTaskData());
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.SENDT_TIL_SORTERING);
    }

    private void opprettVurderSorteringTask(HendelsePayload hendelsePayload, InngåendeHendelse inngåendeHendelse) {
        var nesteKjøringEtter = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(DateUtil.now(),
            inngåendeHendelse);
        LOGGER.info("Hendelse {} med type {} som ble opprettet {} vil bli vurdert på nytt for sortering {}", hendelsePayload.getHendelseId(),
            inngåendeHendelse.getHendelseType().getKode(), hendelsePayload.getHendelseOpprettetTid(), nesteKjøringEtter);
        hendelseRepository.oppdaterHåndteresEtterTidspunkt(inngåendeHendelse, nesteKjøringEtter);
        HendelserDataWrapper vurderSorteringTask = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        vurderSorteringTask.setInngåendeHendelseId(inngåendeHendelse.getId());
        vurderSorteringTask.setHendelseId(hendelsePayload.getHendelseId());
        vurderSorteringTask.setNesteKjøringEtter(nesteKjøringEtter);
        vurderSorteringTask.setHendelseType(inngåendeHendelse.getHendelseType().getKode());
        prosessTaskTjeneste.lagre(vurderSorteringTask.getProsessTaskData());
    }

    private boolean hendelsenErUnderEnUkeGammel(LocalDateTime hendelseOpprettetTid) {
        return hendelseOpprettetTid.plusDays(7).isAfter(DateUtil.now());
    }

    private void ferdigstillHendelseUtenVidereHåndtering(InngåendeHendelse inngåendeHendelse, boolean fjernPayload) {
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
        if (fjernPayload) {
            hendelseRepository.fjernPayload(inngåendeHendelse);
        }
    }

    private HendelseTjeneste<HendelsePayload> getHendelseTjeneste(HendelserDataWrapper dataWrapper, String hendelseType) {
        return hendelseTjenesteProvider.finnTjeneste(HendelseType.fraKode(hendelseType),
            dataWrapper.getHendelseId().orElse(null));
    }
}
