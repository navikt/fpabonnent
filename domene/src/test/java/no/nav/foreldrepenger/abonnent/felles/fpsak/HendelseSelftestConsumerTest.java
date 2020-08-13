package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class HendelseSelftestConsumerTest {
    
    private static final String URL = "https://foo.bar/test/";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    
    @Mock
    public OidcRestClient mockClient;
    
    private HendelseSelftestConsumer consumer;

    private URI endpointUrl;
    
    @Before
    public void setUp() throws Exception {
        endpointUrl = new URI(URL);
        consumer = new HendelseSelftestConsumer(mockClient, endpointUrl);
    }

    @Test
    public void testPing() {
        consumer.ping();
    }

    @Test
    public void testGetEndpointUrl() {
        assertThat(consumer.getEndpointUrl()).isEqualTo(URL + "ping");
    }

}
