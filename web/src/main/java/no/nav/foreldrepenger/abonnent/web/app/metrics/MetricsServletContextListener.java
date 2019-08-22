package no.nav.foreldrepenger.abonnent.web.app.metrics;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

@ApplicationScoped
public class MetricsServletContextListener extends MetricsServlet.ContextListener {

    @Inject
    private MetricRegistry metricRegistry;  // NOSONAR

    @Override
    protected MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }
}
