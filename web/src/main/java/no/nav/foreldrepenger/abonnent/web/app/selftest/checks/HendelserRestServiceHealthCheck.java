package no.nav.foreldrepenger.abonnent.web.app.selftest.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.selftest.HendelseSelftestConsumer;

@ApplicationScoped
public class HendelserRestServiceHealthCheck extends WebServiceHealthCheck {

    private HendelseSelftestConsumer selftestConsumer;

    HendelserRestServiceHealthCheck() {
        // CDI
    }

    @Inject
    public HendelserRestServiceHealthCheck(HendelseSelftestConsumer selftestConsumer) {
        this.selftestConsumer = selftestConsumer;
    }

    @Override
    protected void performWebServiceSelftest() {
        selftestConsumer.ping();
    }

    @Override
    protected String getDescription() {
        return "Test av REST service FPSAK.Hendelser";
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
