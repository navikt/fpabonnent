package no.nav.foreldrepenger.abonnent.felles;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;

public interface HendelseTjeneste<T extends HendelsePayload> {

    T payloadFraString(String payload);

    T payloadFraWrapper(HendelserDataWrapper dataWrapper);

    void populerDatawrapper(T payload, HendelserDataWrapper dataWrapper);

    boolean ikkeAtomiskHendelseSkalSendes(T payload);

    KlarForSorteringResultat vurderOmKlarForSortering(T payload);

    default void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {}
}
