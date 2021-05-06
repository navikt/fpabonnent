package no.nav.foreldrepenger.abonnent.felles.fpsak;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.vedtak.felles.integrasjon.rest.jersey.AbstractJerseyOidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class HendelseConsumer extends AbstractJerseyOidcRestClient {
    private static final String HENDELSE_BASE_ENDPOINT = "fpsakhendelser.v1.url";
    // URI append paths
    private static final String SEND_HENDELSE_PATH = "motta";
    private static final String GROVSORTER_HENDELSE_PATH = "grovsorter";

    private URI baseEndpoint;

    public HendelseConsumer() {
        // CDI
    }

    @Inject
    public HendelseConsumer(@KonfigVerdi(HENDELSE_BASE_ENDPOINT) URI baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    public void sendHendelse(HendelsePayload hendelsePayload) {
        Objects.requireNonNull(hendelsePayload, SEND_HENDELSE_PATH); //$NON-NLS-1$
        HendelseWrapperDto hendelseWrapperDto = hendelsePayload.mapPayloadTilDto();
        client.target(baseEndpoint)
                .path(SEND_HENDELSE_PATH)
                .request(APPLICATION_JSON_TYPE)
                .buildPost(Entity.json(hendelseWrapperDto))
                .invoke(Response.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> grovsorterAktørIder(List<String> aktørIdList) {
        if (!aktørIdList.isEmpty()) {
            List<AktørIdDto> dtoList = aktørIdList.stream().map(AktørIdDto::new).collect(Collectors.toList());
            return client.target(baseEndpoint)
                    .path(GROVSORTER_HENDELSE_PATH)
                    .request(APPLICATION_JSON_TYPE)
                    .buildPost(Entity.json(dtoList))
                    .invoke(Response.class)
                    .readEntity(new GenericType<List<String>>(){});
        }
        return List.of();
    }
}