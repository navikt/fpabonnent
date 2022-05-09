package no.nav.foreldrepenger.abonnent.web.server.abac;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.sikkerhet.abac.AbacAttributtSamling;
import no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpKlient;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;

/**
 * Implementasjon av PDP request for denne applikasjonen.
 */
@ApplicationScoped
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {

    private static final String ABAC_DOMAIN = "foreldrepenger";

    @Override
    public PdpRequest lagPdpRequest(AbacAttributtSamling attributter) {
        PdpRequest pdpRequest = new PdpRequest();
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, attributter.getIdToken());
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE, ABAC_DOMAIN);
        pdpRequest.put(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID, attributter.getActionType().getEksternKode());
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE, attributter.getResource());
        return pdpRequest;
    }
}
