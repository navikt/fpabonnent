package no.nav.foreldrepenger.abonnent.felles.fpsak;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class HendelseConsumer implements Hendelser {
    private static final Logger LOG = LoggerFactory.getLogger(HendelseConsumer.class);

    private OidcRestClient oidcRestClient;
    private URI sendHendelseEndpoint;
    private URI grovsorterEndpoint;

    public HendelseConsumer() {
        // CDI
    }

    @Inject
    public HendelseConsumer(OidcRestClient oidcRestClient, @KonfigVerdi("fpsakhendelser.v1.url") URI baseEndpoint) {
        this.oidcRestClient = oidcRestClient;
        sendHendelseEndpoint = baseEndpoint.resolve("motta");
        grovsorterEndpoint = baseEndpoint.resolve("grovsorter");
    }

    @Override
    public void sendHendelse(HendelsePayload hendelsePayload) {
        Objects.requireNonNull(hendelsePayload, "hendelsePayload");
        HendelseWrapperDto hendelseWrapperDto = hendelsePayload.mapPayloadTilDto();
        LOG.info("Sender hendelse");
        oidcRestClient.post(sendHendelseEndpoint, hendelseWrapperDto);
        LOG.info("Sendt hendelse OK");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> grovsorterAktørIder(List<String> aktørIdList) {
        if (!aktørIdList.isEmpty()) {
            List<AktørIdDto> dtoList = aktørIdList.stream().map(AktørIdDto::new).collect(Collectors.toList());
            LOG.info("Grovsorterer");
            var res = oidcRestClient.post(grovsorterEndpoint, dtoList, List.class);
            LOG.info("Grovsortert OK");
            return res;
        }
        return List.of();
    }
}