package no.nav.foreldrepenger.abonnent.web.server;

import static org.eclipse.jetty.ee11.webapp.MetaInfConfiguration.CONTAINER_JAR_PATTERN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.ee11.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee11.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee11.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.web.app.konfig.ApiConfig;
import no.nav.foreldrepenger.abonnent.web.app.konfig.InternalApiConfig;
import no.nav.foreldrepenger.konfig.Environment;

public class JettyServer {

    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

    private static final String CONTEXT_PATH = ENV.getProperty("context.path", "/fpabonnent");
    private static final String JETTY_SCAN_LOCATIONS = "^.*jersey-.*\\.jar$|^.*felles-.*\\.jar$|^.*/app\\.jar$";
    private static final String JETTY_LOCAL_CLASSES = "^.*/target/classes/|";

    private final Integer serverPort;

    static void main() throws Exception {
        jettyServer().bootStrap();
    }

    protected static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    protected JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    protected void bootStrap() throws Exception {
        var dataSource = DatasourceUtil.createDatasource(30, 2);
        konfigurerDatasource(dataSource);
        migrerDatabase(dataSource);
        start();
    }

    private static void konfigurerDatasource(DataSource dataSource) throws NamingException {
        new EnvEntry("jdbc/defaultDS", dataSource);
    }

    protected void migrerDatabase(DataSource dataSource) {
        try {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/db/migration/defaultDS")
                .table("schema_version")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        } catch (FlywayException e) {
            LOG.error("Feil under migrering av databasen.");
            throw e;
        }
    }

    private void start() throws Exception {
        var server = new Server(getServerPort());
        server.setConnectors(createConnectors(server).toArray(new Connector[]{}));
        server.setHandler(createContext());
        server.start();
        server.join();
    }

    private List<Connector> createConnectors(Server server) {
        List<Connector> connectors = new ArrayList<>();
        var httpConnector = new ServerConnector(server, new HttpConnectionFactory(createHttpConfiguration()));
        httpConnector.setPort(getServerPort());
        connectors.add(httpConnector);
        return connectors;
    }

    private static HttpConfiguration createHttpConfiguration() {
        var httpConfig = new HttpConfiguration();
        // Add support for X-Forwarded headers
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        return httpConfig;
    }

    private static ContextHandler createContext() throws IOException {
        var ctx = new WebAppContext(CONTEXT_PATH, null, simpleConstraints(), null,
            new ErrorPageErrorHandler(), ServletContextHandler.NO_SESSIONS);

        ctx.setParentLoaderPriority(true);

        String baseResource;
        try (var factory = ResourceFactory.closeable()) {
            baseResource = factory.newResource(".").getRealURI().toURL().toExternalForm();
        }
        ctx.setBaseResourceAsString(baseResource);

        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        // Scanns the CLASSPATH for classes and jars.
        ctx.setAttribute(CONTAINER_JAR_PATTERN, String.format("%s%s", ENV.isLocal() ? JETTY_LOCAL_CLASSES : "", JETTY_SCAN_LOCATIONS));

        // Enable Weld + CDI
        ctx.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
        ctx.addServletContainerInitializer(new CdiServletContainerInitializer());
        ctx.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

        ctx.setThrowUnavailableOnStartupException(true);
        return ctx;
    }

    private static ConstraintSecurityHandler simpleConstraints() {
        var handler = new ConstraintSecurityHandler();
        // Slipp gjennom kall fra plattform til JaxRs. Foreløpig kun behov for GET
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, InternalApiConfig.API_URL + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ApiConfig.API_URI + "/*"));
        // Alt annet av paths og metoder forbudt - 403
        handler.addConstraintMapping(pathConstraint(Constraint.FORBIDDEN, "/*"));
        return handler;
    }

    private static ConstraintMapping pathConstraint(Constraint constraint, String path) {
        var mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path);
        return mapping;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
