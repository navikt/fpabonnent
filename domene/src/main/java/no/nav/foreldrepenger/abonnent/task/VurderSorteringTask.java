package no.nav.foreldrepenger.abonnent.task;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.TpsForsinkelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(VurderSorteringTask.TASKNAME)
public class VurderSorteringTask implements ProsessTaskHandler {

    public static final String TASKNAME = "hendelser.vurderSortering";

    private static final Logger LOGGER = LoggerFactory.getLogger(VurderSorteringTask.class);

    private ProsessTaskRepository prosessTaskRepository;
    private TpsForsinkelseTjeneste tpsForsinkelseTjeneste;
    private HendelseTjenesteProvider hendelseTjenesteProvider;
    private HendelseRepository hendelseRepository;

    @Inject
    public VurderSorteringTask(ProsessTaskRepository prosessTaskRepository,
                               TpsForsinkelseTjeneste tpsForsinkelseTjeneste,
                               HendelseTjenesteProvider hendelseTjenesteProvider,
                               HendelseRepository hendelseRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.tpsForsinkelseTjeneste = tpsForsinkelseTjeneste;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        HendelserDataWrapper dataWrapper = new HendelserDataWrapper(prosessTaskData);
        String hendelseType = dataWrapper.getHendelseType()
                .orElseThrow(() -> AbonnentHendelserFeil.FACTORY.ukjentHendelseType(null).toException());
        HendelseTjeneste<HendelsePayload> hendelseTjeneste = getHendelseTjeneste(dataWrapper, hendelseType);

        Optional<Long> inngåendeHendelseId = dataWrapper.getInngåendeHendelseId();
        inngåendeHendelseId.orElseThrow(() -> new IllegalStateException("Prosesstask " + prosessTaskData.getId() + " peker ikke på en gyldig inngående hendelse og kan derfor ikke sorteres videre"));
        InngåendeHendelse inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId.get());
        HendelsePayload hendelsePayload = hendelseTjeneste.payloadFraString(inngåendeHendelse.getPayload());

        KlarForSorteringResultat klarForSorteringResultat = hendelseTjeneste.vurderOmKlarForSortering(hendelsePayload);
        if (klarForSorteringResultat.hendelseKlarForSortering()) {
            hendelseTjeneste.berikHendelseHvisNødvendig(inngåendeHendelse, klarForSorteringResultat);
            opprettSorteringTask(hendelsePayload.getHendelseId(), inngåendeHendelse);
        } else {
            opprettVurderSorteringTaskHvisIkkeHendelsenErForGammel(hendelsePayload, inngåendeHendelse);
        }
    }

    private void opprettSorteringTask(String hendelseId, InngåendeHendelse inngåendeHendelse) {
        HendelserDataWrapper grovsorteringTask = new HendelserDataWrapper(new ProsessTaskData(SorterHendelserTask.TASKNAME));
        grovsorteringTask.setHendelseRequestUuid(hendelseId);
        grovsorteringTask.setHendelseId(hendelseId);
        grovsorteringTask.setInngåendeHendelseId(inngåendeHendelse.getId());
        grovsorteringTask.setHendelseType(inngåendeHendelse.getType().getKode());
        prosessTaskRepository.lagre(grovsorteringTask.getProsessTaskData());
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.SENDT_TIL_SORTERING);
    }

    private void opprettVurderSorteringTaskHvisIkkeHendelsenErForGammel(HendelsePayload hendelsePayload, InngåendeHendelse inngåendeHendelse) {
        if (hendelsenErUnderEnUkeGammel(hendelsePayload.getHendelseOpprettetTid())) {
            LocalDateTime nesteKjøringEtter = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime.now());
            LOGGER.info("Hendelse {} med type {} som ble opprettet {} vil bli vurdert på nytt for sortering {}",
                    hendelsePayload.getHendelseId(), inngåendeHendelse.getType().getKode(), hendelsePayload.getHendelseOpprettetTid(), nesteKjøringEtter);
            HendelserDataWrapper vurderSorteringTask = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
            vurderSorteringTask.setInngåendeHendelseId(inngåendeHendelse.getId());
            vurderSorteringTask.setHendelseId(hendelsePayload.getHendelseId());
            vurderSorteringTask.setNesteKjøringEtter(nesteKjøringEtter);
            vurderSorteringTask.setHendelseType(inngåendeHendelse.getType().getKode());
            prosessTaskRepository.lagre(vurderSorteringTask.getProsessTaskData());
        } else {
            //TODO(JEJ): Kalle "erRegistrert()" for å kunne spisse feilmeldingen (se JOL kommentar på PR)
            LOGGER.warn("Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre",
                    hendelsePayload.getHendelseId(), inngåendeHendelse.getType().getKode(), hendelsePayload.getHendelseOpprettetTid());
            hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
            //TODO(JEJ): Kommentere inn slik at vi fjerner payload når vi har sett at det fungerer (+ bestille patch til null på gamle):
            //hendelseRepository.fjernPayload(inngåendeHendelse);
        }
    }

    private boolean hendelsenErUnderEnUkeGammel(LocalDateTime hendelseOpprettetTid) {
        return hendelseOpprettetTid.plusDays(7).isAfter(LocalDateTime.now());
    }

    private HendelseTjeneste<HendelsePayload> getHendelseTjeneste(HendelserDataWrapper dataWrapper, String hendelseType) {
        return hendelseTjenesteProvider.finnTjeneste(HendelseType.fraKode(hendelseType), dataWrapper.getHendelseId().orElse(null));
    }
}
