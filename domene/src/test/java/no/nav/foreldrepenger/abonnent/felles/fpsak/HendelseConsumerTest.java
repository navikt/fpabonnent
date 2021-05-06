package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.foreldrepenger.felles.integrasjon.rest.DefaultJsonMapper.MAPPER;
import static no.nav.vedtak.felles.integrasjon.rest.jersey.AbstractJerseyRestClient.DEFAULT_NAV_CALLID;
import static no.nav.vedtak.felles.integrasjon.rest.jersey.AbstractJerseyRestClient.OIDC_AUTH_HEADER_PREFIX;
import static no.nav.vedtak.log.mdc.MDCOperations.generateCallId;
import static no.nav.vedtak.log.mdc.MDCOperations.putCallId;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ExtendWith(MockitoExtension.class)
public class HendelseConsumerTest {

    private static final String CALLID = generateCallId();

    private static final String TOKEN = "TOKEN";

    private static URI baseEndpoint = URI.create("http://localhost:8080");
    private HendelseConsumer consumer = new HendelseConsumer(baseEndpoint);

    private static WireMockServer wireMockServer = new WireMockServer(8080);

    @Mock
    private SubjectHandler subjectHandler;

    @BeforeAll
    static void startServer() {
        putCallId(CALLID);
        wireMockServer.start();
        configureFor(wireMockServer.port());
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void beforeEach() {
        lenient().doReturn(TOKEN).when(subjectHandler).getInternSsoToken();
    }

    @Test
    public void skal_videresende_fødselshendelse() {
        stubFor(headers(post(urlPathEqualTo("/motta")))
                .willReturn(emptyResponse()));
        try (var s = mockStatic(SubjectHandler.class)) {
            s.when(SubjectHandler::getSubjectHandler).thenReturn(subjectHandler);
            consumer.sendHendelse(HendelseTestDataUtil.lagFødselsHendelsePayload());
            System.out.println("Sendt");
        }
    }

    @Test
    public void skal_returnere_tom_liste()  {
         var res = consumer.grovsorterAktørIder(List.of());
         assertTrue(res.isEmpty());
    }

    @Test
    public void skal_videresende_aktørId_som_dto() throws JsonProcessingException {
        List<String> idList = List.of("1", "2", "3");
        List<String> idListSortert = List.of("1", "3");

        try (var s = mockStatic(SubjectHandler.class)) {
            s.when(SubjectHandler::getSubjectHandler).thenReturn(subjectHandler);
            stubFor(headers(post(urlPathEqualTo("/grovsorter")).withPort(wireMockServer.port()))
                    .willReturn(responseBody(idListSortert)));
            var res = consumer.grovsorterAktørIder(idList);
            assertEquals(idListSortert, res);
        }
    }

    private static ResponseDefinitionBuilder emptyResponse() {
        return aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    private static ResponseDefinitionBuilder responseBody(Object body) throws JsonProcessingException {
        return aResponse()
                .withStatus(SC_OK)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(body));
    }

    private static MappingBuilder headers(MappingBuilder p) {
        return p.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .withHeader(AUTHORIZATION, containing(OIDC_AUTH_HEADER_PREFIX))
                .withHeader(AUTHORIZATION, containing(TOKEN))
                .withHeader(DEFAULT_NAV_CALLID, equalTo(CALLID));
    }

}
