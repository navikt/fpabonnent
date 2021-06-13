package no.nav.foreldrepenger.abonnent.felles.fpsak;

import java.util.List;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;

public interface Hendelser {

    void sendHendelse(HendelsePayload hendelse);

    List<String> grovsorterAktørIder(List<String> aktører);

}
