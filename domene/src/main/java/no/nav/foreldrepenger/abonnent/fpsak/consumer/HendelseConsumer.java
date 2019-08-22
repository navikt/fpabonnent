package no.nav.foreldrepenger.abonnent.fpsak.consumer;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class HendelseConsumer {
    private static final String HENDELSE_BASE_ENDPOINT = "fpsakhendelser_v1.url";
    // URI append paths
    private static final String SEND_HENDELSE_PATH = "hendelse";
    private static final String GROVSORTER_HENDELSE_PATH = "grovsorter";

    private OidcRestClient oidcRestClient;
    private HendelseMapper hendelseMapper;
    private URI baseEndpoint;
    private URI sendHendelseEndpoint;
    private URI grovsorterEndpoint;

    public HendelseConsumer() {
        // CDI
    }

    @Inject
    public HendelseConsumer(OidcRestClient oidcRestClient, @KonfigVerdi(HENDELSE_BASE_ENDPOINT) URI baseEndpoint,
                            HendelseMapper hendelseMapper) {
        this.oidcRestClient = oidcRestClient;
        this.baseEndpoint = baseEndpoint;
        this.hendelseMapper = hendelseMapper;
        sendHendelseEndpoint = this.baseEndpoint.resolve(SEND_HENDELSE_PATH);
        grovsorterEndpoint = this.baseEndpoint.resolve(GROVSORTER_HENDELSE_PATH);
    }

    public void sendHendelse(HendelsePayload hendelsePayload) {
        Objects.requireNonNull(hendelsePayload, SEND_HENDELSE_PATH); //$NON-NLS-1$
        HendelseWrapperDto hendelseWrapperDto = hendelseMapper.map(hendelsePayload);
        oidcRestClient.post(sendHendelseEndpoint, hendelseWrapperDto);
    }

    @SuppressWarnings("unchecked")
    public List<String> grovsorterAktørIder(List<String> aktørIdList) {
        if (!aktørIdList.isEmpty()) {
            List<AktørIdDto> dtoList = aktørIdList.stream().map(AktørIdDto::new).collect(Collectors.toList());
            return oidcRestClient.post(grovsorterEndpoint, dtoList, List.class);
        }
        return Collections.emptyList();
    }
}