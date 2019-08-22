package no.nav.foreldrepenger.abonnent.web.app.selftest;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

public class HealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    private HealthCheckRegistry healthCheckRegistry;

    public HealthCheckServletContextListener() {
        // for CDi
    }

    @Inject
    public HealthCheckServletContextListener(HealthCheckRegistry healthCheckRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);
        ContextPathHolder.instance(sce.getServletContext().getContextPath());
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }
}
