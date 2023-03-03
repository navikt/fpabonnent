package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import java.time.LocalDateTime;

/**
 * Muliggjør testing av klasser der nåtid har funksjonell betydning
 */
public class DateUtil {
    private DateUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
