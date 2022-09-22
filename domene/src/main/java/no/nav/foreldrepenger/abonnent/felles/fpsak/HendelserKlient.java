package no.nav.foreldrepenger.abonnent.felles.fpsak;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, application = FpApplication.FPSAK)
public class HendelserKlient {

    private static final String API_PATH = "/api/hendelser";
    private RestClient restKlient;
    private URI motta;
    private URI grovsorter;

    HendelserKlient() {
        // CDI
    }

    @Inject
    public HendelserKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.motta = UriBuilder.fromUri(RestConfig.contextPathFromAnnotation(HendelserKlient.class)).path(API_PATH).path("motta").build();
        this.grovsorter = UriBuilder.fromUri(RestConfig.contextPathFromAnnotation(HendelserKlient.class)).path(API_PATH).path("grovsorter").build();
    }

    public void sendHendelse(HendelsePayload h) {
        restKlient.sendReturnOptional(RestRequest.newPOSTJson(h.mapPayloadTilDto(), motta, HendelserKlient.class), String.class);
    }

    public List<String> grovsorterAktørIder(List<String> aktører) {
        if (!aktører.isEmpty()) {
            var dtos = aktører.stream().map(AktørIdDto::new).toList();
            var respons = restKlient.send(RestRequest.newPOSTJson(dtos, grovsorter, TokenFlow.STS_CC, null), String[].class);
            return Arrays.asList(respons);
        }
        return List.of();
    }
}
