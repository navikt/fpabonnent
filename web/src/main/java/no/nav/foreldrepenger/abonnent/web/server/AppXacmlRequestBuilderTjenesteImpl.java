package no.nav.foreldrepenger.abonnent.web.server;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;

import no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.pdp.XacmlRequestBuilderTjeneste;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlAttributeSet;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlRequestBuilder;

@Dependent
@Alternative
@Priority(2)
public class AppXacmlRequestBuilderTjenesteImpl implements XacmlRequestBuilderTjeneste {

    @Override
    public XacmlRequestBuilder lagXacmlRequestBuilder(PdpRequest pdpRequest) {
        XacmlRequestBuilder xacmlBuilder = new XacmlRequestBuilder();

        XacmlAttributeSet actionAttributeSet = new XacmlAttributeSet();
        actionAttributeSet.addAttribute(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID, pdpRequest.getString(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID));
        xacmlBuilder.addActionAttributeSet(actionAttributeSet);

        XacmlAttributeSet resourceAttributeSet = new XacmlAttributeSet();
        resourceAttributeSet.addAttribute(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE, pdpRequest.getString(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE));
        resourceAttributeSet.addAttribute(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE, pdpRequest.getString(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE));
        xacmlBuilder.addResourceAttributeSet(resourceAttributeSet);

        return xacmlBuilder;
    }
}