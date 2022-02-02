package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.vedtak.felles.integrasjon.rest.jersey.AbstractJerseyOidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@Dependent
@Jersey
public class JerseyHendelser extends AbstractJerseyOidcRestClient implements Hendelser {

    private final URI baseUri;

    @Inject
    public JerseyHendelser(@KonfigVerdi(value = "fpsakhendelser.v1.url", defaultVerdi = "http://fpsak/fpsak/api/hendelser") URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public void sendHendelse(HendelsePayload h) {
        invoke(client.target(baseUri)
                .path("motta")
                .request(APPLICATION_JSON_TYPE)
                .buildPost(json(h.mapPayloadTilDto())));
    }

    @Override
    public List<String> grovsorterAktørIder(List<String> aktører) {
        if (!aktører.isEmpty()) {
            return invoke(client.target(baseUri)
                    .path("grovsorter")
                    .request(APPLICATION_JSON_TYPE)
                    .buildPost(json(aktører.stream()
                            .map(AktørIdDto::new)
                            .collect(toList()))),
                    new GenericType<List<String>>() {
                    });
        }
        return List.of();
    }
}
