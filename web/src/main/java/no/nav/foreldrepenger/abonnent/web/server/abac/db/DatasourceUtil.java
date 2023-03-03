package no.nav.foreldrepenger.abonnent.web.server.abac.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.core.instrument.Metrics;
import no.nav.foreldrepenger.konfig.Environment;

import javax.sql.DataSource;

import java.util.Properties;

public class DatasourceUtil {

    private DatasourceUtil() {
    }

    private static final Environment ENV = Environment.current();

    public static DataSource createDatasource(int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ENV.getRequiredProperty("defaultDS.url"));
        config.setUsername(ENV.getRequiredProperty("defaultDS.username"));
        config.setPassword(ENV.getRequiredProperty("defaultDS.password"));
        config.setConnectionTimeout(1000);
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTestQuery("select 1 from dual");
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMetricRegistry(Metrics.globalRegistry);

        Properties dsProperties = new Properties();
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }
}
