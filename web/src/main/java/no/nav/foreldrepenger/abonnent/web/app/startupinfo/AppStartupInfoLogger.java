package no.nav.foreldrepenger.abonnent.web.app.startupinfo;

import static com.google.common.collect.Maps.fromProperties;
import static java.util.Map.Entry.comparingByKey;
import static no.nav.vedtak.konfig.StandardPropertySource.APP_PROPERTIES;
import static no.nav.vedtak.konfig.StandardPropertySource.ENV_PROPERTIES;
import static no.nav.vedtak.konfig.StandardPropertySource.SYSTEM_PROPERTIES;

import java.util.List;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import no.nav.foreldrepenger.abonnent.web.app.selftest.Selftests;
import no.nav.foreldrepenger.abonnent.web.app.selftest.checks.ExtHealthCheck;
import no.nav.vedtak.konfig.StandardPropertySource;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
class AppStartupInfoLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AppStartupInfoLogger.class);

    private Selftests selftests;

    private static final String OPPSTARTSINFO = "OPPSTARTSINFO";
    private static final String HILITE_SLUTT = "********";
    private static final String HILITE_START = HILITE_SLUTT;
    private static final String SELFTEST = "Selftest";
    private static final String APPLIKASJONENS_STATUS = "Applikasjonens status";
    private static final String START = "start:";
    private static final String SLUTT = "slutt.";

    AppStartupInfoLogger() {
    }

    @Inject
    AppStartupInfoLogger(Selftests selftests) {
        this.selftests = selftests;
    }

    void logAppStartupInfo() {
        log(HILITE_START + " " + OPPSTARTSINFO + " " + START + " " + HILITE_SLUTT);
        logSelftest();
        log(HILITE_START + " " + OPPSTARTSINFO + " " + SLUTT + " " + HILITE_SLUTT);
    }

    private void logSelftest() {
        log(SELFTEST + " " + START);

        // callId er påkrevd på utgående kall og må settes før selftest kjøres
        MDCOperations.putCallId();
        var samletResultat = selftests.run();
        MDCOperations.removeCallId();

        samletResultat.getAlleResultater().stream()
                .forEach(AppStartupInfoLogger::log);

        log(APPLIKASJONENS_STATUS + ": {}", samletResultat.getAggregateResult());
        log(SELFTEST + " " + SLUTT);
    }

    private static void log(String msg, Object... args) {
        log(false, msg, args);
    }

    private static void log(boolean ignore, String msg, Object... args) {
        if (ignore) {
            LOG.debug(msg, args);
        } else {
            LOG.info(msg, args);
        }
    }

    private static void log(HealthCheck.Result result) {
        if (result.getDetails() != null) {
            OppstartFeil.FACTORY.selftestStatus(
                    getStatus(result.isHealthy()),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_DESCRIPTION),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_ENDPOINT),
                    (String) result.getDetails().get(ExtHealthCheck.DETAIL_RESPONSE_TIME),
                    result.getMessage()).log(LOG);
        } else {
            OppstartFeil.FACTORY.selftestStatus(
                    getStatus(result.isHealthy()),
                    null,
                    null,
                    null,
                    result.getMessage()).log(LOG);
        }
    }

    private static String getStatus(boolean isHealthy) {
        return isHealthy ? "OK" : "ERROR";
    }
}