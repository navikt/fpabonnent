package no.nav.foreldrepenger.abonnent.pdl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TpsForsinkelseTjeneste {

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

    public TpsForsinkelseTjeneste() {
        // CDI
    }

    public LocalDateTime finnNesteTidspunktForVurderSortering(LocalDateTime opprettetTid) {
        if (DayOfWeek.FRIDAY.equals(opprettetTid.getDayOfWeek())) {
            return finnNesteÅpningsdag(opprettetTid.plusDays(4));
        } else if (DayOfWeek.SATURDAY.equals(opprettetTid.getDayOfWeek())) {
            return finnNesteÅpningsdag(opprettetTid.plusDays(3));
        } else {
            return finnNesteÅpningsdag(opprettetTid.plusDays(2));
        }
    }

    public LocalDateTime finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime sistKjøringTid) {
        return finnNesteÅpningsdag(sistKjøringTid.plusDays(1));
    }

    private LocalDateTime finnNesteÅpningsdag(LocalDateTime utgangspunkt) {
        if (!HELGEDAGER.contains(utgangspunkt.getDayOfWeek()) && !erFastRødDag(utgangspunkt.toLocalDate())) {
            return getTidspunktMellom0630og0659(utgangspunkt);
        } else {
            return finnNesteÅpningsdag(utgangspunkt.plusDays(1));
        }
    }

    private LocalDateTime getTidspunktMellom0630og0659(LocalDateTime utgangspunkt) {
        return LocalDateTime.of(utgangspunkt.toLocalDate(),
                OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739));
    }

    private boolean erFastRødDag(LocalDate dato) {
        return FASTE_STENGT_DAGER.contains(MonthDay.from(dato));
    }
}
