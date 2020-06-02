package no.nav.foreldrepenger.abonnent.pdl;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TpsForsinkelseTjeneste {

    // Velger et tidspunkt litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Set<DayOfWeek> HELGEDAGER = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public TpsForsinkelseTjeneste() {
        // CDI
    }

    public LocalDateTime finnNesteTidspunktForVurderSortering(LocalDateTime opprettetTid) {
        LocalDateTime toDagerFrem = getTidspunktMellom0630og0659(opprettetTid, 2);

        if (HELGEDAGER.contains(toDagerFrem.getDayOfWeek())) {
            return getTidspunktMellom0630og0659(opprettetTid, 4);
        } else if (DayOfWeek.SATURDAY.equals(opprettetTid.getDayOfWeek())) {
            return getTidspunktMellom0630og0659(opprettetTid, 3);
        }

        //TODO(JEJ): Legge inn liste over rød-dager som skal hoppes over

        return toDagerFrem;
    }

    public LocalDateTime finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime sistKjøringTid) {
        LocalDateTime enDagFrem = getTidspunktMellom0630og0659(sistKjøringTid, 1);

        if (HELGEDAGER.contains(enDagFrem.getDayOfWeek())) {
            return finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(sistKjøringTid.plusDays(1));
        }
        //TODO(JEJ): Legge inn liste over rød-dager som skal hoppes over

        return enDagFrem;
    }

    private LocalDateTime getTidspunktMellom0630og0659(LocalDateTime utgangspunkt, int antallDagerFremITid) {
        return LocalDateTime.of(utgangspunkt.plusDays(antallDagerFremITid).toLocalDate(),
                OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739));
    }
}
