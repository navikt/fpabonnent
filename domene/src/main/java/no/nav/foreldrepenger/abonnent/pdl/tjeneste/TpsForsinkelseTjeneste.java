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
 * Tjenesten forsinker hendelser mens man venter på at de skal bli tilgjengelige i batch-oppdaterte TPS,
 * typisk 1-2 arbeidsdager etter PDL. Den sørger også for å ivareta rekkefølgen når hendelser er koblet, slik
 * at historikkinnslagene i FPSAK får riktig rekkefølge.
 *
 * Når FPSAK har byttet til PDL kan man i utgangspunktet sende hendelsene mye tidligere, men det er trolig
 * at det fremdeles må beholdes en viss forsinkelse, feks pga oppførsel der DSF bruker ANNULLERT+OPPRETTET
 * når de skal gjøre korrigeringer. Det er da ikke ønsket at ANNULLERT-hendelsen skal slippes før grunnlaget
 * er klart med den korrigerte informasjonen. Det kan også være aktuelt å fremdeles unngå hendelser på natten
 * når Oppdrag er stengt. Disse behovene må utredes nærmere når det blir aktuelt å erstatte denne tjenestens
 * nåværende logikk med kortere ventetid.
 */
@ApplicationScoped
public class TpsForsinkelseTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(TpsForsinkelseTjeneste.class);

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

    private HendelseRepository hendelseRepository;

    public TpsForsinkelseTjeneste() {
        // CDI
    }

    @Inject
    public TpsForsinkelseTjeneste(HendelseRepository hendelseRepository) {
        this.hendelseRepository = hendelseRepository;
    }

    public LocalDateTime finnNesteTidspunktForVurderSortering(LocalDateTime opprettetTid, InngåendeHendelse inngåendeHendelse) {
        Optional<LocalDateTime> tidspunktBasertPåTidligereHendelse = sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse);
        return tidspunktBasertPåTidligereHendelse.orElseGet(() -> doFinnNesteTidspunktForVurderSortering(opprettetTid));
    }

    public LocalDateTime finnNesteTidspunktForVurderSorteringEtterFørsteKjøring(LocalDateTime sistKjøringTid, InngåendeHendelse inngåendeHendelse) {
        Optional<LocalDateTime> tidspunktBasertPåTidligereHendelse = sjekkOmHendelsenMåKjøreEtterTidligereHendelse(inngåendeHendelse);
        return tidspunktBasertPåTidligereHendelse.orElseGet(() -> finnNesteÅpningsdag(sistKjøringTid.plusDays(1)));
    }

    private Optional<LocalDateTime> sjekkOmHendelsenMåKjøreEtterTidligereHendelse(InngåendeHendelse inngåendeHendelse) {
        if (inngåendeHendelse.getTidligereHendelseId() != null) {
            Optional<InngåendeHendelse> tidligereHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(inngåendeHendelse.getTidligereHendelseId(), inngåendeHendelse.getHendelseKilde());
            if (tidligereHendelse.isPresent() && !HåndtertStatusType.HÅNDTERT.equals(tidligereHendelse.get().getHåndtertStatus())) {
                LocalDateTime tidspunktBasertPåTidligereHendelse = tidligereHendelse.get().getHåndteresEtterTidspunkt().plusMinutes(2);
                LOGGER.info("Hendelse {} har en tidligere hendelse {} som ikke er håndtert {} og vil derfor bli behandlet {}",
                        inngåendeHendelse.getHendelseId(), inngåendeHendelse.getTidligereHendelseId(), tidligereHendelse.get().getHåndteresEtterTidspunkt(), tidspunktBasertPåTidligereHendelse);
                return Optional.of(tidspunktBasertPåTidligereHendelse);
            }
        }
        return Optional.empty();
    }

    private LocalDateTime doFinnNesteTidspunktForVurderSortering(LocalDateTime opprettetTid) {
        if (DayOfWeek.FRIDAY.equals(opprettetTid.getDayOfWeek())) {
            return finnNesteÅpningsdag(opprettetTid.plusDays(4));
        } else if (DayOfWeek.SATURDAY.equals(opprettetTid.getDayOfWeek())) {
            return finnNesteÅpningsdag(opprettetTid.plusDays(3));
        } else {
            return finnNesteÅpningsdag(opprettetTid.plusDays(2));
        }
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
