package no.nav.foreldrepenger.abonnent.felles.fpsak;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPSAK)
public class HendelserKlient {

    private static final String API_PATH = "/api/hendelser";
    private final RestClient restKlient;
    private final RestConfig restConfig;
    private final URI motta;
    private final URI grovsorter;
    private final URI grovsorterHistorisk;

    public HendelserKlient() {
        this(RestClient.client());
    }

    HendelserKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.motta = UriBuilder.fromUri(restConfig.fpContextPath()).path(API_PATH).path("motta").build();
        this.grovsorter = UriBuilder.fromUri(restConfig.fpContextPath()).path(API_PATH).path("grovsorter").build();
        this.grovsorterHistorisk = UriBuilder.fromUri(restConfig.fpContextPath()).path(API_PATH).path("grovsorter-historisk").build();
    }

    public void sendHendelse(HendelsePayload h) {
        var request = RestRequest.newPOSTJson(h.mapPayloadTilDto(), motta, restConfig);
        restKlient.sendReturnOptional(request, String.class);
    }

    public Set<String> grovsorterAktørIder(Set<String> aktører) {
        if (!aktører.isEmpty()) {
            var dtos = aktører.stream().map(AktørIdDto::new).toList();
            var request = RestRequest.newPOSTJson(dtos, grovsorter, restConfig);
            var respons = restKlient.send(request, String[].class);
            return new HashSet<>(Arrays.asList(respons));
        }
        return Set.of();
    }

    public Set<String> grovsorterHistorisk(Set<String> aktører) {
        if (!aktører.isEmpty()) {
            var dtos = aktører.stream().map(AktørIdDto::new).toList();
            var request = RestRequest.newPOSTJson(dtos, grovsorterHistorisk, restConfig);
            var respons = restKlient.send(request, String[].class);
            return new HashSet<>(Arrays.asList(respons));
        }
        return Set.of();
    }
}
