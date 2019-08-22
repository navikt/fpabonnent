package no.nav.foreldrepenger.abonnent.web.app.selftest.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.selftest.TpsFeedSelftestConsumer;

@ApplicationScoped
public class TpsFeedRestServiceHealthCheck extends WebServiceHealthCheck {

    private TpsFeedSelftestConsumer selftestConsumer;

    TpsFeedRestServiceHealthCheck() {
        // CDI
    }

    @Inject
    public TpsFeedRestServiceHealthCheck(TpsFeedSelftestConsumer selftestConsumer) {
        this.selftestConsumer = selftestConsumer;
    }

    @Override
    protected void performWebServiceSelftest() {
        selftestConsumer.ping();
    }

    @Override
    protected String getDescription() {
        return "Test av REST service TpsFeed";
    }

    @Override
    protected String getEndpoint() {
        return selftestConsumer.getEndpointUrl();
    }
    
    @Override
    public boolean erKritiskTjeneste() {
        return false;
    }
}
