package no.nav.foreldrepenger.abonnent.selftest;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.tjenester.person.feed.common.v1.Feed;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class TpsFeedSelftestConsumerImpl implements TpsFeedSelftestConsumer {

    private static final String HENDELSE_BASE_ENDPOINT = "person-feed-v2.url";

    private OidcRestClient oidcRestClient;
    private URI endpointUrl;

    TpsFeedSelftestConsumerImpl() {
        // CDI
    }

    @Inject
    public TpsFeedSelftestConsumerImpl(OidcRestClient oidcRestClient, @KonfigVerdi(HENDELSE_BASE_ENDPOINT) URI endpointUrl) throws URISyntaxException {
        this.endpointUrl = new URIBuilder(endpointUrl).addParameter("sequenceId", "1").addParameter("pageSize", "1").build();
        this.oidcRestClient = oidcRestClient;
    }

    @Override
    public void ping() {
        oidcRestClient.get(endpointUrl, Feed.class);
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl.normalize().toString();
    }
}
