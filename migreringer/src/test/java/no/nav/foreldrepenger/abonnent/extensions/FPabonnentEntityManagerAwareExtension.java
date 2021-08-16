package no.nav.foreldrepenger.abonnent.extensions;


import static no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering.migrer;
import static no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering.settJdniOppslag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;
import no.nav.foreldrepenger.konfig.Environment;

public class FPabonnentEntityManagerAwareExtension extends EntityManagerAwareExtension {
    private static final Logger LOG = LoggerFactory.getLogger(FPabonnentEntityManagerAwareExtension.class);
    private static final boolean isNotRunningUnderMaven = Environment.current().getProperty("maven.cmd.line.args") == null;

    static {
        if (isNotRunningUnderMaven) {
            LOG.info("Kjører IKKE under maven");
            // prøver alltid migrering hvis endring, ellers funker det dårlig i IDE.
            migrer();
        }
        settJdniOppslag();
    }

}