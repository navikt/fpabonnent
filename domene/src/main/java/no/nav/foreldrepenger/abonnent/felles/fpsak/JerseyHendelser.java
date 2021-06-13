package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.GenericType;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.vedtak.felles.integrasjon.rest.jersey.AbstractJerseyOidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@Dependent
@Jersey
public class JerseyHendelser extends AbstractJerseyOidcRestClient implements Hendelser {
    private static final String HENDELSE_BASE_ENDPOINT = "fpsakhendelser.v1.url";
    private static final String SEND_HENDELSE_PATH = "motta";
    private static final String GROVSORTER_HENDELSE_PATH = "grovsorter";

    private final URI baseUri;

    public JerseyHendelser(@KonfigVerdi(value = HENDELSE_BASE_ENDPOINT, defaultVerdi = "some sensible default") URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public void sendHendelse(HendelsePayload h) {
        invoke(client.target(baseUri)
                .path(SEND_HENDELSE_PATH)
                .request(APPLICATION_JSON_TYPE)
                .buildPost(json(h.mapPayloadTilDto())));
    }

    @Override
    public List<String> grovsorterAktørIder(List<String> aktører) {
        if (!aktører.isEmpty()) {
            return invoke(client.target(baseUri)
                    .path(GROVSORTER_HENDELSE_PATH)
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
