package no.nav.foreldrepenger.abonnent.web.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.MetaData;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.web.app.konfig.ApplicationConfig;
import no.nav.foreldrepenger.abonnent.web.server.DataSourceKonfig.DBConnProp;
import no.nav.vedtak.isso.IssoApplication;
import no.nav.vedtak.isso.config.ServerInfo;
import no.nav.vedtak.util.env.Environment;

public class JettyServer extends AbstractJettyServer {

    private static final Environment ENV = Environment.current();

    private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

    private DataSourceKonfig dataSourceKonfig;

    public JettyServer() {
        this(new JettyWebKonfigurasjon());
    }

    public JettyServer(int serverPort) {
        this(new JettyWebKonfigurasjon(serverPort));
    }

    JettyServer(AppKonfigurasjon appKonfigurasjon) {
        super(appKonfigurasjon);
    }

    public static void main(String[] args) throws Exception {
        JettyServer jettyServer;
        if (args.length > 0) {
            int serverPort = Integer.parseUnsignedInt(args[0]);
            jettyServer = new JettyServer(serverPort);
        } else {
            jettyServer = new JettyServer();
        }
        jettyServer.bootStrap();
    }

    @Override
    protected void konfigurerMiljø() throws Exception {
        dataSourceKonfig = new DataSourceKonfig();
        hacks4Nais();
        LOG.info("Serverinfo 2", ServerInfo.instance().toString());
    }

    private void hacks4Nais() {
        loadBalancerFqdnTilLoadBalancerUrl();
        temporært();
    }

    private void loadBalancerFqdnTilLoadBalancerUrl() {
        ENV.getProperty("LOADBALANCER_FQDN");
        if (ENV.getProperty("LOADBALANCER_FQDN") != null) {
            String loadbalancerFqdn = ENV.getProperty("LOADBALANCER_FQDN");
            String protocol = (loadbalancerFqdn.startsWith("localhost")) ? "http" : "https";
            System.setProperty("loadbalancer.url", protocol + "://" + loadbalancerFqdn);
            LOG.info("Setter loadbalancer.url fra {} til {}", loadbalancerFqdn, ENV.getProperty("loadbalancer.url"));
        }
    }

    private void temporært() {
        // FIXME : PFP-1176 Skriv om i OpenAmIssoHealthCheck og
        // AuthorizationRequestBuilder når Jboss dør
        if (System.getenv("OIDC_OPENAM_HOSTURL") != null) {
            System.setProperty("OpenIdConnect.issoHost", System.getenv("OIDC_OPENAM_HOSTURL"));
        }
        // FIXME : PFP-1176 Skriv om i AuthorizationRequestBuilder og
        // IdTokenAndRefreshTokenProvider når Jboss dør
        if (System.getenv("OIDC_OPENAM_AGENTNAME") != null) {
            System.setProperty("OpenIdConnect.username", System.getenv("OIDC_OPENAM_AGENTNAME"));
        }
        // FIXME : PFP-1176 Skriv om i IdTokenAndRefreshTokenProvider når Jboss dør
        if (System.getenv("OIDC_OPENAM_PASSWORD") != null) {
            System.setProperty("OpenIdConnect.password", System.getenv("OIDC_OPENAM_PASSWORD"));
        }
    }

    @Override
    protected void konfigurerJndi() throws Exception {
        new EnvEntry("jdbc/defaultDS", dataSourceKonfig.getDefaultDatasource().getDatasource());
    }

    @Override
    protected void migrerDatabaser() throws IOException {
        for (DBConnProp dbConnProp : dataSourceKonfig.getDataSources()) {
            new DatabaseScript(dbConnProp.getDatasource(), false, dbConnProp.getMigrationScripts()).migrate();
        }
    }

    @Override
    protected WebAppContext createContext(AppKonfigurasjon appKonfigurasjon) throws IOException {
        WebAppContext webAppContext = super.createContext(appKonfigurasjon);
        webAppContext.setParentLoaderPriority(true);
        updateMetaData(webAppContext.getMetaData());
        return webAppContext;
    }

    private void updateMetaData(MetaData metaData) {
        // Find path to class-files while starting jetty from development environment.
        List<Class<?>> appClasses = getWebInfClasses();

        List<Resource> resources = appClasses.stream()
                .map(c -> Resource.newResource(c.getProtectionDomain().getCodeSource().getLocation()))
                .distinct()
                .collect(Collectors.toList());

        metaData.setWebInfClassesDirs(resources);
    }

    protected List<Class<?>> getWebInfClasses() {
        return Arrays.asList(ApplicationConfig.class, IssoApplication.class);
    }

    @Override
    protected ResourceCollection createResourceCollection() throws IOException {
        return new ResourceCollection(
                Resource.newClassPathResource("META-INF/resources/webjars/"),
                Resource.newClassPathResource("/web"));
    }
}
