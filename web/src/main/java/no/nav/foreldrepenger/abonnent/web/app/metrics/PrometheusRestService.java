package no.nav.foreldrepenger.abonnent.web.app.metrics;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.prometheus.client.CollectorRegistry;
import io.swagger.v3.oas.annotations.Operation;
import no.nav.vedtak.log.metrics.MetricsUtil;

@Path("/metrics")
@ApplicationScoped
@Produces(MediaType.TEXT_PLAIN)
public class PrometheusRestService {

    @GET
    @Operation(tags = "metrics", hidden = true)
    @Path("/prometheus")
    public String prometheus() {
        return MetricsUtil.scrape();
    }
}
