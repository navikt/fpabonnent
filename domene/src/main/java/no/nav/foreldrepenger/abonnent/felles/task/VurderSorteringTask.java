package no.nav.foreldrepenger.abonnent.felles.task;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
@ProsessTask("hendelser.vurderSortering")
public class VurderSorteringTask implements ProsessTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VurderSorteringTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForsinkelseTjeneste forsinkelseTjeneste;
    private HendelseTjenesteProvider hendelseTjenesteProvider;
    private HendelseRepository hendelseRepository;

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
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        String hendelseType = dataWrapper.getHendelseType()
                .orElseThrow(AbonnentHendelserFeil::ukjentHendelseType);
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = getHendelseTjeneste(dataWrapper, hendelseType);

        Optional<Long> inngåendeHendelseId = dataWrapper.getInngåendeHendelseId();
        inngåendeHendelseId.orElseThrow(() -> new IllegalStateException("Prosesstask " + prosessTaskData.getId() + " peker ikke på en gyldig inngående hendelse og kan derfor ikke sorteres videre"));
        InngåendeHendelse inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId.get());
        HendelsePayload hendelsePayload = hendelseTjeneste.payloadFraJsonString(inngåendeHendelse.getPayload());

        if (hendelseTjeneste.vurderOmHendelseKanForkastes(hendelsePayload)) {
            ferdigstillHendelseUtenVidereHåndtering(inngåendeHendelse, true);
            return;
        }

        if (enTidligereHendelseLiggerUbehandlet(inngåendeHendelse)) {
            opprettVurderSorteringTask(hendelsePayload, inngåendeHendelse);
            return;
        }

        KlarForSorteringResultat klarForSorteringResultat = hendelseTjeneste.vurderOmKlarForSortering(hendelsePayload);
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

    private boolean enTidligereHendelseLiggerUbehandlet(InngåendeHendelse inngåendeHendelse) {
        // Scenario som kanskje kan oppstå hvis hendelsene kommer samtidig på Kafka, og blir plukket av forskjellige noder samtidig
        if (inngåendeHendelse.getTidligereHendelseId() != null) {
            Optional<InngåendeHendelse> tidligereHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(inngåendeHendelse.getTidligereHendelseId(), inngåendeHendelse.getHendelseKilde());
            if (tidligereHendelse.isPresent() && !HåndtertStatusType.HÅNDTERT.equals(tidligereHendelse.get().getHåndtertStatus())) {
                LOGGER.info("Hendelse {} har en tidligere hendelse {} som ikke er håndtert enda, og vil derfor vente til den er ferdig",
                        inngåendeHendelse.getHendelseId(), tidligereHendelse.get().getHendelseId());
                return true;
            }
        }
        return false;
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
        LocalDateTime nesteKjøringEtter = forsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now(), inngåendeHendelse);
        LOGGER.info("Hendelse {} med type {} som ble opprettet {} vil bli vurdert på nytt for sortering {}",
                hendelsePayload.getHendelseId(), inngåendeHendelse.getHendelseType().getKode(), hendelsePayload.getHendelseOpprettetTid(), nesteKjøringEtter);
        hendelseRepository.oppdaterHåndteresEtterTidspunkt(inngåendeHendelse, nesteKjøringEtter);
        HendelserDataWrapper vurderSorteringTask = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        vurderSorteringTask.setInngåendeHendelseId(inngåendeHendelse.getId());
        vurderSorteringTask.setHendelseId(hendelsePayload.getHendelseId());
        vurderSorteringTask.setNesteKjøringEtter(nesteKjøringEtter);
        vurderSorteringTask.setHendelseType(inngåendeHendelse.getHendelseType().getKode());
        prosessTaskTjeneste.lagre(vurderSorteringTask.getProsessTaskData());
    }

    private boolean hendelsenErUnderEnUkeGammel(LocalDateTime hendelseOpprettetTid) {
        return hendelseOpprettetTid.plusDays(7).isAfter(LocalDateTime.now());
    }

    private void ferdigstillHendelseUtenVidereHåndtering(InngåendeHendelse inngåendeHendelse, boolean fjernPayload) {
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
        if (fjernPayload) {
            hendelseRepository.fjernPayload(inngåendeHendelse);
        }
    }

    private HendelseTjeneste<HendelsePayload> getHendelseTjeneste(HendelserDataWrapper dataWrapper, String hendelseType) {
        return hendelseTjenesteProvider.finnTjeneste(HendelseType.fraKode(hendelseType), dataWrapper.getHendelseId().orElse(null));
    }
}
