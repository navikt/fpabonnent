package no.nav.foreldrepenger.abonnent.selftest;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdFeedSelftestConsumerImpl implements InfotrygdFeedSelftestConsumer  {

    private static final String INFOTRYGD_FEED_BASE_ENDPOINT = "infotrygd.hendelser.api.url";

    private OidcRestClient oidcRestClient;
    private URI endpointUrl;

    InfotrygdFeedSelftestConsumerImpl() {
        // CDI
    }

    @Inject
    public InfotrygdFeedSelftestConsumerImpl(OidcRestClient oidcRestClient, @KonfigVerdi(INFOTRYGD_FEED_BASE_ENDPOINT) URI endpointUrl) throws URISyntaxException {
        this.endpointUrl = new URIBuilder(endpointUrl).addParameter("sistLesteSekvensId", "0").addParameter("maxAntall", "1").build();
        this.oidcRestClient = oidcRestClient;
    }

    @Override
    public void ping() {
        oidcRestClient.get(endpointUrl, Object.class);
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl.normalize().toString();
    }
}
