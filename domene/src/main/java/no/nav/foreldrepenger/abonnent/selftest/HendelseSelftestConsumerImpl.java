package no.nav.foreldrepenger.abonnent.selftest;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
class HendelseSelftestConsumerImpl implements HendelseSelftestConsumer {

    private static final String HENDELSE_BASE_ENDPOINT = "fpsakhendelser.v1.url";

    private OidcRestClient oidcRestClient;
    private URI endpointUrl;

    HendelseSelftestConsumerImpl() {
        // CDI
    }

    @Inject
    public HendelseSelftestConsumerImpl(OidcRestClient oidcRestClient, @KonfigVerdi(HENDELSE_BASE_ENDPOINT) URI endpointUrl) {
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
