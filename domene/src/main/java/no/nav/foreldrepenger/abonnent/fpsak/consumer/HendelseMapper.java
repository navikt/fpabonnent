package no.nav.foreldrepenger.abonnent.fpsak.consumer;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;

@ApplicationScoped
public class HendelseMapper {
    public static final String FØDSEL_HENDELSE_TYPE = "FØDSEL";
    public static final String DØD_HENDELSE_TYPE = "DØD";
    public static final String DØDFØDSEL_HENDELSE_TYPE = "DØDFØDSEL";

    public HendelseWrapperDto map(HendelsePayload payload) {
        return payload.mapPayloadTilDto();
    }

}
