package no.nav.foreldrepenger.abonnent.web.app.tjenester;

import no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseHealthCheckTest {

    @Test
    public void test_check_healthy() {
        Databaseskjemainitialisering.initUnitTestDataSource();
        DatabaseHealthCheck dbCheck = new DatabaseHealthCheck();

        assertThat(dbCheck.isReady()).isTrue();
    }

    @Test
    public void skal_feile_pga_ukjent_jndi_name() {
        DatabaseHealthCheck dbCheck = new DatabaseHealthCheck("jndi/ukjent");

        assertThat(dbCheck.isReady()).isFalse();
    }

}
