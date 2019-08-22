package no.nav.foreldrepenger.abonnent.web.app.selftest.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.selftest.InfotrygdFeedSelftestConsumer;

@ApplicationScoped
public class InfotrygdFeedRestServiceHealthCheck extends WebServiceHealthCheck {

    private InfotrygdFeedSelftestConsumer selftestConsumer;

    InfotrygdFeedRestServiceHealthCheck() {
        // CDI
    }

    @Inject
    public InfotrygdFeedRestServiceHealthCheck(InfotrygdFeedSelftestConsumer selftestConsumer) {
        this.selftestConsumer = selftestConsumer;
    }

    @Override
    protected void performWebServiceSelftest() {
        selftestConsumer.ping();
    }

    @Override
    protected String getDescription() {
        return "Test av REST service Infotrygd";
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
