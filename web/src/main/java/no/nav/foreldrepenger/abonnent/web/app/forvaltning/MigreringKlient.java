package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPMOTTAK)
public class MigreringKlient {

    private static final String API_PATH = "/api/migrer/hendelse";
    private final RestClient restKlient;
    private final RestConfig restConfig;
    private final URI migrer;


    public MigreringKlient() {
        this(RestClient.client());
    }

    MigreringKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.migrer = UriBuilder.fromUri(restConfig.fpContextPath()).path(API_PATH).build();
    }

    public void sendHendelse(MigreringHendelseDto.HendelseDto h) {
        var request = RestRequest.newPOSTJson(h, migrer, restConfig);
        restKlient.sendReturnOptional(request, String.class);
    }

}
