package no.nav.foreldrepenger.abonnent.tjenester;

import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.FeedKode;

public interface InngåendeHendelseTjeneste {

    List<InngåendeHendelse> hentHendelserSomErSendtTilSorteringMedUUID(String uuid);

    void oppdaterHåndtertStatus(InngåendeHendelse inngåendeHendelse, HåndtertStatusType håndtertStatusType);

    void oppdaterHendelseSomSendtNå(HendelsePayload hendelsePayload);

    void markerIkkeRelevanteHendelserSomHåndtert(Map<Long, InngåendeHendelse> inngåendeHendelserMap, List<HendelsePayload> relevanteHendelser);

    List<HendelsePayload> getPayloadsForInngåendeHendelser(List<InngåendeHendelse> inngåendeHendelser);

    List<InngåendeHendelse> finnAlleIkkeSorterteHendelserFraFeed(FeedKode feedKode);
}
