package no.nav.foreldrepenger.abonnent.feed.poller;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;

class PollerUtils {

    private PollerUtils() {
    }

    public static boolean klarTilÅKjøres(InputFeed konfig) {
        Optional<LocalDateTime> sistLest = konfig.getSistLest();
        Optional<LocalDateTime> sistFeilet = konfig.getSistFeilet();
        boolean harAldriKjørtFør = !sistLest.isPresent() && !sistFeilet.isPresent();
        if (harAldriKjørtFør) {
            return true;
        }
        boolean sisteKjøringFeilet = sistFeilet.isPresent() && (!sistLest.isPresent() || sistFeilet.get().isAfter(sistLest.get()));
        LocalDateTime nesteTidspunkt;
        if (sisteKjøringFeilet) {
            nesteTidspunkt = sistFeilet.get().plus(konfig.getVentetidFeilet());
        } else if (konfig.getNextUrl().isPresent()) {
            nesteTidspunkt = sistLest.get().plus(konfig.getVentetidLesbar());
        } else {
            nesteTidspunkt = sistLest.get().plus(konfig.getVentetidFerdiglest());
        }
        return LocalDateTime.now().isAfter(nesteTidspunkt);
    }

}
