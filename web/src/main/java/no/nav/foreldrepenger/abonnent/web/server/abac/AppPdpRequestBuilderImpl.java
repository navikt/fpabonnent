package no.nav.foreldrepenger.abonnent.web.server.abac;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;
import no.nav.vedtak.sikkerhet.abac.pdp.AppRessursData;

/**
 * Implementasjon av PDP request for denne applikasjonen.
 */
@ApplicationScoped
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {

    @Override
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        return AppRessursData.builder().build();
    }
}
