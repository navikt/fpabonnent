package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import java.time.LocalDateTime;

import javax.enterprise.context.Dependent;

/**
 * Muliggjør testing av klasser der nåtid har funksjonell betydning
 */
@Dependent
public class DateUtil {
    public DateUtil() {
    }

    public LocalDateTime nå() {
        return LocalDateTime.now();
    }
}
