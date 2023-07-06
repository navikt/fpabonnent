package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;

/**
 * Tjenesten forsinker hendelser i minst 1 time pga oppførsel der det benyttes ANNULLERT+OPPRETTET til korrigeringer.
 * Det er ikke ønsket at ANNULLERT-hendelsen skal slippes før grunnlaget er klart med den korrigerte informasjonen.
 * <p>
 * Videre ønsker vi å unngå hendelser på faste stengt dager, helger, og natten når Oppdrag er stengt.
 */
@ApplicationScoped
public class ForsinkelseTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForsinkelseTjeneste.class);

    // Velger et tidspunkt litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Set<DayOfWeek> HELGEDAGER = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private static final Set<MonthDay> FASTE_STENGT_DAGER = Set.of(MonthDay.of(1, 1), MonthDay.of(5, 1), MonthDay.of(5, 17), MonthDay.of(12, 25),
        MonthDay.of(12, 26), MonthDay.of(12, 31));

    private ForsinkelseKonfig forsinkelseKonfig;
    private HendelseRepository hendelseRepository;
    private DateUtil dateUtil;

    public ForsinkelseTjeneste() {
        // CDI
    }

    @Inject
    public ForsinkelseTjeneste(ForsinkelseKonfig forsinkelseKonfig, HendelseRepository hendelseRepository, DateUtil dateUtil) {
        this.forsinkelseKonfig = forsinkelseKonfig;
        this.hendelseRepository = hendelseRepository;
        this.dateUtil = dateUtil;
    }

    public LocalDateTime nå() {
        return dateUtil.nå();
    }

    public LocalDateTime finnNesteTidspunktForVurderSortering(InngåendeHendelse inngåendeHendelse) {
        if (!forsinkelseKonfig.skalForsinkeHendelser()) {
            return dateUtil.nå();
        }
        return sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse)
            .orElseGet(this::doFinnNesteTidspunktForVurderSortering);
    }

    public LocalDateTime finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime sistKjøringTid, InngåendeHendelse inngåendeHendelse) {
        return sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse)
            .orElseGet(() -> finnNesteVurderingstid(sistKjøringTid.plusDays(1)));
    }

    private Optional<LocalDateTime> sjekkOmHendelsenMåKjøreEtterTidligereHendelse(InngåendeHendelse inngåendeHendelse) {
        return Optional.ofNullable(inngåendeHendelse.getTidligereHendelseId())
            .flatMap(thid -> hendelseRepository.finnHendelseFraIdHvisFinnes(thid, inngåendeHendelse.getHendelseKilde()))
            .filter(th -> !HåndtertStatusType.HÅNDTERT.equals(th.getHåndtertStatus()))
            .map(th -> utledTidFraTidligereHendelse(inngåendeHendelse, th));
    }

    private LocalDateTime utledTidFraTidligereHendelse(InngåendeHendelse inngåendeHendelse, InngåendeHendelse tidligereHendelse) {
        var tidspunktBasertPåTidligereHendelse = tidligereHendelse.getHåndteresEtterTidspunkt().plusMinutes(2);
        if (dateUtil.nå().isAfter(tidspunktBasertPåTidligereHendelse)) {
            LocalDateTime nesteDagEtterRetryAll = dateUtil.nå().plusDays(1).withHour(7).withMinute(30);
            LOGGER.info(
                "Hendelse {} har en tidligere hendelse {} som skulle vært håndtert {}, men ikke er det, og vil derfor bli forsøkt behandlet igjen i morgen etter retry all: {}",
                inngåendeHendelse.getHendelseId(), inngåendeHendelse.getTidligereHendelseId(),
                tidligereHendelse.getHåndteresEtterTidspunkt(), nesteDagEtterRetryAll);
            return nesteDagEtterRetryAll;
        } else {
            LOGGER.info("Hendelse {} har en tidligere hendelse {} som ikke er håndtert {} og vil derfor bli behandlet {}",
                inngåendeHendelse.getHendelseId(), inngåendeHendelse.getTidligereHendelseId(),
                tidligereHendelse.getHåndteresEtterTidspunkt(), tidspunktBasertPåTidligereHendelse);
            return tidspunktBasertPåTidligereHendelse;
        }
    }

    private LocalDateTime doFinnNesteTidspunktForVurderSortering() {
        var tid = dateUtil.nå().plusMinutes(forsinkelseKonfig.normalForsinkelseMinutter());
        if (tid.isBefore(tid.with(OPPDRAG_VÅKNER))) {
            return finnNesteVurderingstid(tid);
        } else if (tid.isAfter(tid.withHour(22).withMinute(58)) || erStengtDag(tid)) {
            return finnNesteVurderingstid(tid.plusDays(1));
        } else {
            return tid;
        }
    }

    private LocalDateTime finnNesteVurderingstid(LocalDateTime utgangspunkt) {
        if (!erStengtDag(utgangspunkt)) {
            return getTidspunktMellom0630og0659(utgangspunkt.toLocalDate());
        } else {
            return finnNesteVurderingstid(utgangspunkt.plusDays(1));
        }
    }

    private boolean erStengtDag(LocalDateTime tid) {
        return HELGEDAGER.contains(tid.getDayOfWeek()) || FASTE_STENGT_DAGER.contains(MonthDay.from(tid));
    }

    private LocalDateTime getTidspunktMellom0630og0659(LocalDate utgangspunkt) {
        return LocalDateTime.of(utgangspunkt, OPPDRAG_VÅKNER.plusSeconds(dateUtil.nå().getNano() % 1739));
    }
}
