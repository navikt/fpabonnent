package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;

/**
 * Tjenesten forsinker hendelser i minst 1 time pga oppførsel der det benyttes ANNULLERT+OPPRETTET til korrigeringer.
 * Det er ikke ønsket at ANNULLERT-hendelsen skal slippes før grunnlaget er klart med den korrigerte informasjonen.
 *
 * Videre ønsker vi å unngå hendelser på faste stengt dager, helger, og natten når Oppdrag er stengt.
 */
@ApplicationScoped
public class ForsinkelseTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForsinkelseTjeneste.class);

    // Velger et tidspunkt litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Set<DayOfWeek> HELGEDAGER = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private static final Set<MonthDay> FASTE_STENGT_DAGER = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(5, 17),
            MonthDay.of(12, 25),
            MonthDay.of(12, 26),
            MonthDay.of(12, 31)
    );

    private ForsinkelseKonfig forsinkelseKonfig;
    private HendelseRepository hendelseRepository;

    public ForsinkelseTjeneste() {
        // CDI
    }

    @Inject
    public ForsinkelseTjeneste(ForsinkelseKonfig forsinkelseKonfig, HendelseRepository hendelseRepository) {
        this.forsinkelseKonfig = forsinkelseKonfig;
        this.hendelseRepository = hendelseRepository;
    }

    public LocalDateTime finnNesteTidspunktForVurderSortering(InngåendeHendelse inngåendeHendelse) {
        if (!forsinkelseKonfig.skalForsinkeHendelser()) {
            return LocalDateTime.now();
        }
        Optional<LocalDateTime> tidspunktBasertPåTidligereHendelse = sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse);
        return tidspunktBasertPåTidligereHendelse.orElseGet(this::doFinnNesteTidspunktForVurderSortering);
    }

    public LocalDateTime finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime sistKjøringTid, InngåendeHendelse inngåendeHendelse) {
        Optional<LocalDateTime> tidspunktBasertPåTidligereHendelse = sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse);
        return tidspunktBasertPåTidligereHendelse.orElseGet(() -> finnNesteÅpningsdag(sistKjøringTid.toLocalDate().plusDays(1)));
    }

    private Optional<LocalDateTime> sjekkOmHendelsenMåKjøreEtterTidligereHendelse(InngåendeHendelse inngåendeHendelse) {
        if (inngåendeHendelse.getTidligereHendelseId() != null) {
            Optional<InngåendeHendelse> tidligereHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(inngåendeHendelse.getTidligereHendelseId(), inngåendeHendelse.getHendelseKilde());
            if (tidligereHendelse.isPresent() && !HåndtertStatusType.HÅNDTERT.equals(tidligereHendelse.get().getHåndtertStatus())) {
                LocalDateTime tidspunktBasertPåTidligereHendelse = tidligereHendelse.get().getHåndteresEtterTidspunkt().plusMinutes(2);
                if (LocalDateTime.now().isAfter(tidspunktBasertPåTidligereHendelse)) {
                    LocalDateTime nesteDagEtterRetryAll = LocalDateTime.now().plusDays(1).withHour(7).withMinute(30).withSecond(0).withNano(0);
                    LOGGER.info("Hendelse {} har en tidligere hendelse {} som skulle vært håndtert {}, men ikke er det, og vil derfor bli forsøkt behandlet igjen i morgen etter retry all: {}",
                            inngåendeHendelse.getHendelseId(), inngåendeHendelse.getTidligereHendelseId(), tidligereHendelse.get().getHåndteresEtterTidspunkt(), nesteDagEtterRetryAll);
                    return Optional.of(nesteDagEtterRetryAll);
                } else {
                    LOGGER.info("Hendelse {} har en tidligere hendelse {} som ikke er håndtert {} og vil derfor bli behandlet {}",
                            inngåendeHendelse.getHendelseId(), inngåendeHendelse.getTidligereHendelseId(), tidligereHendelse.get().getHåndteresEtterTidspunkt(), tidspunktBasertPåTidligereHendelse);
                    return Optional.of(tidspunktBasertPåTidligereHendelse);
                }
            }
        }
        return Optional.empty();
    }

    private LocalDateTime doFinnNesteTidspunktForVurderSortering() {
        LocalDate dagensDato = DateUtil.now().toLocalDate();
        if (stengtTidNå() || erStengtDag(dagensDato)) {
            return finnNesteÅpningsdag(dagensDato.plusDays(1));
        } else {
            return DateUtil.now().plusHours(1);
        }
    }

    private LocalDateTime finnNesteÅpningsdag(LocalDate utgangspunkt) {
        if (!erStengtDag(utgangspunkt)) {
            return getTidspunktMellom0630og0659(utgangspunkt);
        } else {
            return finnNesteÅpningsdag(utgangspunkt.plusDays(1));
        }
    }

    private boolean stengtTidNå() {
        return DateUtil.now().isAfter(DateUtil.now().withHour(23).withMinute(30))
                || DateUtil.now().isBefore(DateUtil.now().withHour(6).withMinute(30));
    }

    private boolean erStengtDag(LocalDate dato) {
        return HELGEDAGER.contains(dato.getDayOfWeek()) || erFastRødDag(dato);
    }

    private LocalDateTime getTidspunktMellom0630og0659(LocalDate utgangspunkt) {
        return LocalDateTime.of(utgangspunkt,
                OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739));
    }

    private boolean erFastRødDag(LocalDate dato) {
        return FASTE_STENGT_DAGER.contains(MonthDay.from(dato));
    }
}
