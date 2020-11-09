package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ExtendWith(MockitoExtension.class)
public class HendelseSelftestConsumerTest {
    
    private static final String URL = "https://foo.bar/test/";


    @Mock
    public OidcRestClient mockClient;
    
    private HendelseSelftestConsumer consumer;

    private URI endpointUrl;
    
    @BeforeEach
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
