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
import no.nav.vedtak.felles.integrasjon.rest.NativeClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestCompact;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@NativeClient
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, application = FpApplication.FPSAK)
public class NativeHendelser implements Hendelser {

    private static final String API_PATH = "/api/hendelser";
    private final RestCompact restKlient;
    private final URI motta;
    private final URI grovsorter;

    @Inject
    public NativeHendelser(RestCompact restKlient) {
        this.restKlient = restKlient;
        this.motta = UriBuilder.fromUri(RestConfig.endpointFromAnnotation(NativeHendelser.class)).path(API_PATH).path("motta").build();
        this.grovsorter = UriBuilder.fromUri(RestConfig.endpointFromAnnotation(NativeHendelser.class)).path(API_PATH).path("grovsorter").build();
    }

    @Override
    public void sendHendelse(HendelsePayload h) {
        restKlient.postString(NativeHendelser.class, motta, h.mapPayloadTilDto());
    }

    @Override
    public List<String> grovsorterAktørIder(List<String> aktører) {
        if (!aktører.isEmpty()) {
            var dtos = aktører.stream().map(AktørIdDto::new).toList();
            var respons = restKlient.postValue(grovsorter, TokenFlow.STS_CC, null, dtos, String[].class);
            return Arrays.asList(respons);
        }
        return List.of();
    }
}
