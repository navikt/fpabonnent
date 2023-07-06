package no.nav.foreldrepenger.abonnent.web.app.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
