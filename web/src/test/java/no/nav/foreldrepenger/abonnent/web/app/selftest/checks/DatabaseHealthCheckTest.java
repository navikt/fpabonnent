package no.nav.foreldrepenger.abonnent.web.app.selftest.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import javax.naming.NameNotFoundException;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.web.app.selftest.checks.ExtHealthCheck.InternalResult;

public class DatabaseHealthCheckTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
    }

    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    @Test
    public void test_check_healthy() {
        DatabaseHealthCheck dbCheck = new DatabaseHealthCheck();

        InternalResult result = dbCheck.performCheck();

        assertThat(result.isOk()).isTrue();
        assertThat(result.getResponseTimeMs()).isNotNull();
    }

    @Test
    public void skal_feile_pga_ukjent_jndi_name() {
        DatabaseHealthCheck dbCheck = new DatabaseHealthCheck("jndi/ukjent");

        InternalResult result = dbCheck.performCheck();

        assertThat(result.isOk()).isFalse();
        assertThat(result.getMessage()).contains("Feil ved JNDI-oppslag for jndi/ukjent");
        assertThat(result.getException()).isInstanceOf(NameNotFoundException.class);
    }

}
