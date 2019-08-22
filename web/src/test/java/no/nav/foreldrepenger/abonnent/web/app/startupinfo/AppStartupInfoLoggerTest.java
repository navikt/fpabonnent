package no.nav.foreldrepenger.abonnent.web.app.startupinfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.codahale.metrics.health.HealthCheck;

import no.nav.foreldrepenger.abonnent.web.app.selftest.SelftestResultat;
import no.nav.foreldrepenger.abonnent.web.app.selftest.Selftests;
import no.nav.foreldrepenger.abonnent.web.app.selftest.checks.ExtHealthCheck;
import no.nav.modig.core.test.LogSniffer;
import no.nav.vedtak.sikkerhet.loginmodule.ContainerLogin;

public class AppStartupInfoLoggerTest {

    @Rule
    public final LogSniffer logSniffer = new LogSniffer();
    
    private AppStartupInfoLogger logger;

    @Before
    public void setup() {

        SelftestResultat samletResultat = new SelftestResultat();

        samletResultat.setApplication("minApp");
        samletResultat.setVersion("0.1");
        samletResultat.setTimestamp(LocalDateTime.now());
        samletResultat.setRevision("old");
        samletResultat.setBuildTime("long ago");

        HealthCheck.ResultBuilder builder = HealthCheck.Result.builder();
        builder.healthy();
        builder.withDetail(ExtHealthCheck.DETAIL_DESCRIPTION, "descr1");
        builder.withDetail(ExtHealthCheck.DETAIL_RESPONSE_TIME, "90ms");
        builder.withDetail(ExtHealthCheck.DETAIL_ENDPOINT, "http://ws.nav.no");
        samletResultat.leggTilResultatForIkkeKritiskTjeneste(builder.build());
        samletResultat.leggTilResultatForIkkeKritiskTjeneste(HealthCheck.Result.unhealthy("no2"));

        Selftests mockSelftests = mock(Selftests.class);
        when(mockSelftests.run()).thenReturn(samletResultat);
        ContainerLogin mockContainerLogin = Mockito.mock(ContainerLogin.class);

        logger = new AppStartupInfoLogger(mockSelftests, mockContainerLogin);
    }

    @Test
    public void test() {
        logger.logAppStartupInfo();
        
        logSniffer.assertHasInfoMessage("OPPSTARTSINFO start");
        logSniffer.assertHasInfoMessage("System property");
        logSniffer.assertHasInfoMessage("OPPSTARTSINFO slutt");
        logSniffer.assertNoErrorsOrWarnings();

    }
}
