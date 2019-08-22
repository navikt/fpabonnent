package no.nav.foreldrepenger.abonnent.web.server;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseScript.class);
            
    private final DataSource dataSource;
    private final boolean cleanOnException;
    private final String locations;

    public DatabaseScript(DataSource dataSource, boolean cleanOnException, String locations) {
        this.dataSource = dataSource;
        this.cleanOnException = cleanOnException;
        this.locations = locations;
    }

    public void migrate() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setBaselineOnMigrate(true);
        flyway.setLocations(locations);

        try {
            flyway.migrate();
        } catch (FlywayException e) {
            LOGGER.info("Flyway-migrerering feilet", e);
            // prøv en gang til
            if (cleanOnException) {
                flyway.clean();
                flyway.migrate();
            }
        }

    }
}
