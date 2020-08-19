package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;

public interface HendelseTjeneste<T extends HendelsePayload> {

    T payloadFraJsonString(String payload);

    default boolean vurderOmHendelseKanForkastes(T payload) {
        return false;
    }

    KlarForSorteringResultat vurderOmKlarForSortering(T payload);

    default void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {}

    default void loggFeiletHendelse(T payload) {}
}
