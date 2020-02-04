package no.nav.foreldrepenger.abonnent.dbstøtte;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class UnittestRepositoryRule extends RepositoryRule {

    private static final Logger log = LoggerFactory.getLogger(UnittestRepositoryRule.class);
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }
    static {
        if (System.getenv("MAVEN_CMD_LINE_ARGS") == null) {
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            log.warn("Kjører migreringer");
            Databaseskjemainitialisering.migrerUnittestSkjemaer();
        } else {
            // Maven kjører testen
            // kun kjør migreringer i migreringer modul
        }

        Databaseskjemainitialisering.settPlaceholdereOgJdniOppslag();
    }

    @Override
    protected void init() {
    }
}
