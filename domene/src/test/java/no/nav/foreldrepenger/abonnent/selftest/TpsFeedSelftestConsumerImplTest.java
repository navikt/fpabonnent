package no.nav.foreldrepenger.abonnent.selftest;



import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class TpsFeedSelftestConsumerImplTest {
    
    private static final String URL = "https://foo.bar/tpsfeed";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    
    @Mock
    public OidcRestClient mockClient;
    
    private TpsFeedSelftestConsumer consumer;

    private URI endpointUrl;
    
    @Before
    public void setUp() throws Exception {
        endpointUrl = new URI(URL);
        consumer = new TpsFeedSelftestConsumerImpl(mockClient, endpointUrl); 
    }

    @Test
    public void testPing() {
        consumer.ping();
    }

    @Test
    public void testGetEndpointUrl() {
        assertThat(consumer.getEndpointUrl()).contains(URL + "?sequenceId=1&pageSize=1");
    }

}
