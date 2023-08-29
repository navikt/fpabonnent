package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import jakarta.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.person.Tema;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "pdl.base.url", endpointDefault = "http://pdl-api.pdl/graphql", scopesProperty = "pdl.scopes", scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
public class PdlKlient extends AbstractPersonKlient {

    public PdlKlient() {
        super(RestClient.client(), Tema.FOR);
    }

    public PdlKlient(Tema tema) {
        super(RestClient.client(), tema);
    }

}
