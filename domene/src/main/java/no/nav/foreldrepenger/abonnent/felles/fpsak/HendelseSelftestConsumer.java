package no.nav.foreldrepenger.abonnent.felles.fpsak;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestSelftestConsumer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class HendelseSelftestConsumer implements RestSelftestConsumer {

    private OidcRestClient oidcRestClient;
    private URI endpointUrl;

    HendelseSelftestConsumer() {
        // CDI
    }

    @Inject
    public HendelseSelftestConsumer(OidcRestClient oidcRestClient, @KonfigVerdi("fpsakhendelser.v1.url") URI endpointUrl) {
        this.endpointUrl = endpointUrl.resolve("ping");
        this.oidcRestClient = oidcRestClient;
    }

    @Override
    public void ping() {
        oidcRestClient.post(endpointUrl, null);
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl.normalize().toString();
    }
}
