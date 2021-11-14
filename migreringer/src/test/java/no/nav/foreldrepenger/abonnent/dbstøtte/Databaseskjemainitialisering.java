package no.nav.foreldrepenger.abonnent.dbstøtte;

import java.io.File;

import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.foreldrepenger.konfig.Environment;

/**
 * Initielt skjemaoppsett + migrering av unittest-skjemaer
 */
public final class Databaseskjemainitialisering {

    private static final Logger LOG = LoggerFactory.getLogger(Databaseskjemainitialisering.class);
    private static final Environment ENV = Environment.current();

    public static final DBProperties DEFAULT_DS_PROPERTIES = dbProperties("defaultDS", "fpabonnent");
    public static final String URL_DEFAULT = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp) (HOST=127.0.0.1)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=XE)))";
    protected static final String SCHEMA_VERSION_TABLE = "schema_version";

    public static void main(String[] args) {
        migrer();
    }

    public static void migrer() {
        migrer(DEFAULT_DS_PROPERTIES);
    }

    public static DBProperties defaultProperties() {
        return DEFAULT_DS_PROPERTIES;
    }

    private static DBProperties dbProperties(String dsName, String schema) {
        return new DBProperties(dsName, schema, ds(dsName, schema), getScriptLocation(dsName));
    }

    public static void settJdniOppslag() {
        try {
            var props = DEFAULT_DS_PROPERTIES;
            new EnvEntry("jdbc/" + props.dsName(), props.dataSource());
        } catch (Exception e) {
            throw new RuntimeException("Feil under registrering av JDNI-entry for default datasource", e);
        }
    }

    private static void migrer(DBProperties dbProperties) {
        LOG.info("Migrerer {}", dbProperties.schema());
        var flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(dbProperties.dataSource())
                .table(SCHEMA_VERSION_TABLE)
                .locations(dbProperties.scriptLocation())
                .cleanOnValidationError(true)
                .load();
        if (!ENV.isLocal()) {
            throw new IllegalStateException("Forventer at denne migreringen bare kjøres lokalt");
        }
        flyway.migrate();
    }

    private static String getScriptLocation(String dsName) {
        if (DBTestUtil.kjøresAvMaven()) {
            return classpathScriptLocation(dsName);
        }
        return fileScriptLocation(dsName);
    }

    private static String classpathScriptLocation(String dsName) {
        return "classpath:/db/migration/" + dsName;
    }

    private static String fileScriptLocation(String dsName) {
        var relativePath = "migreringer/src/main/resources/db/migration/" + dsName;
        var baseDir = new File(".").getAbsoluteFile();
        var location = new File(baseDir, relativePath);
        while (!location.exists()) {
            baseDir = baseDir.getParentFile();
            if (baseDir == null || !baseDir.isDirectory()) {
                throw new IllegalArgumentException("Klarte ikke finne : " + baseDir);
            }
            location = new File(baseDir, relativePath);
        }
        return "filesystem:" + location.getPath();
    }

    public static DataSource ds(String dsName, String schema) {
        var ds = new HikariDataSource(hikariConfig(dsName, schema));
        Runtime.getRuntime().addShutdownHook(new Thread(ds::close));
        return ds;
    }

    private static HikariConfig hikariConfig(String dsName, String schema) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(ENV.getProperty(dsName + ".url", URL_DEFAULT));
        cfg.setUsername(ENV.getProperty(dsName + ".username", schema));
        cfg.setPassword(ENV.getProperty(dsName + ".password", schema));
        cfg.setConnectionTimeout(10000);
        cfg.setMinimumIdle(0);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);
        return cfg;
    }

    public static record DBProperties(String dsName, String schema, DataSource dataSource, String scriptLocation) {
    }
}
