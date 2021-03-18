package no.nav.foreldrepenger.abonnent.web.server;

import static no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering.migrer;
import static no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering.settJdniOppslag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import no.nav.foreldrepenger.abonnent.dbstøtte.Databaseskjemainitialisering;
import no.nav.foreldrepenger.abonnent.web.app.konfig.ApplicationConfig;
import no.nav.vedtak.isso.IssoApplication;
import no.nav.vedtak.util.env.Environment;

/**
 * Setter opp jetty automatisk lokalt med riktig konfig verdier.
 */
public class JettyDevServer extends AbstractJettyServer {

    private static final Environment ENV = Environment.current();
    private static final String TRUSTSTORE_PASSW_PROP = "javax.net.ssl.trustStorePassword";
    private static final String TRUSTSTORE_PATH_PROP = "javax.net.ssl.trustStore";

    public JettyDevServer(JettyWebKonfigurasjon webKonfigurasjon) {
        super(webKonfigurasjon);
    }

    public static void main(String[] args) throws Exception {
        new JettyDevServer(new JettyWebKonfigurasjon(8065)).bootStrap();
    }

    @Override
    protected void konfigurer() throws Exception {
        System.setProperty("conf", "src/main/resources/jetty/");
        super.konfigurer();
    }

    @Override
    protected void konfigurerJndi() {
        settJdniOppslag();
    }

    @Override
    protected void konfigurerMiljø() {
    }

    @Override
    protected void migrerDatabaser() {
        migrer();
    }

    @Override
    protected void konfigurerSikkerhet() {
        initCryptoStoreConfig();
        super.konfigurerSikkerhet();
    }

    @Override
    protected List<Connector> createConnectors(JettyWebKonfigurasjon appKonfigurasjon, Server server) {
        var connectors = super.createConnectors(appKonfigurasjon, server);
        var https = createHttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        var sslConnector = new ServerConnector(server,
                new SslConnectionFactory(new SslContextFactory.Server(), "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(8059);
        connectors.add(sslConnector);
        return connectors;
    }

    @Override
    protected WebAppContext createContext(JettyWebKonfigurasjon appKonfigurasjon) throws IOException {
        var ctx = super.createContext(appKonfigurasjon);
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        return ctx;
    }

    @Override
    protected List<Class<?>> getWebInfClasses() {
        return List.of(ApplicationConfig.class, IssoApplication.class);
    }

    private static void initCryptoStoreConfig() {
        var defaultLocation = ENV.getProperty("user.home", ".") + "/.modig/truststore.jks";

        var storePath = ENV.getProperty(JettyDevServer.TRUSTSTORE_PATH_PROP, defaultLocation);
        var storeFile = new File(storePath);
        if (!storeFile.exists()) {
            throw new IllegalStateException("Finner ikke " + "truststore" + " i " + storePath
                    + "\n\tKonfigurer enten som System property '" + JettyDevServer.TRUSTSTORE_PATH_PROP + "' eller environment variabel '"
                    + JettyDevServer.TRUSTSTORE_PATH_PROP.toUpperCase().replace('.', '_') + "'");
        }
        var password = ENV.getProperty(JettyDevServer.TRUSTSTORE_PASSW_PROP, "changeit");
        if (password == null) {
            throw new IllegalStateException("Passord for å aksessere store " + "truststore" + " i " + storePath + " er null");
        }

        System.setProperty(JettyDevServer.TRUSTSTORE_PATH_PROP, storeFile.getAbsolutePath());
        System.setProperty(JettyDevServer.TRUSTSTORE_PASSW_PROP, password);
    }
}
