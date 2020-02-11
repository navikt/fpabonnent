package no.nav.foreldrepenger.abonnent.tjenester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjenesteProvider;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;

@ApplicationScoped
public class InngåendeHendelseTjeneste {

    private HendelseRepository hendelseRepository;
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    InngåendeHendelseTjeneste() {
        // CDI
    }

    @Inject
    public InngåendeHendelseTjeneste(HendelseRepository hendelseRepository, HendelseTjenesteProvider hendelseTjenesteProvider) {
        this.hendelseRepository = hendelseRepository;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
    }

    public List<InngåendeHendelse> hentHendelserSomErSendtTilSorteringMedUUID(String uuid) {
        Objects.requireNonNull(uuid, "mangler request UUID for inngående hendelser");  //$NON-NLS-1$
        return hendelseRepository.finnHendelserSomErSendtTilSorteringMedRequestUUID(uuid);
    }

    public void oppdaterHåndtertStatus(InngåendeHendelse inngåendeHendelse, HåndtertStatusType håndtertStatusType) {
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, håndtertStatusType);
    }

    public void oppdaterHendelseSomSendtNå(HendelsePayload hendelsePayload) {
        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(hendelsePayload.getFeedKode(), hendelsePayload.getSekvensnummer());
        if (hendelse.isPresent()) {
            hendelseRepository.markerHendelseSomSendtNå(hendelse.get());
            hendelseRepository.oppdaterHåndtertStatus(hendelse.get(), HåndtertStatusType.HÅNDTERT);
        }
    }

    public void markerIkkeRelevanteHendelserSomHåndtert(Map<Long, InngåendeHendelse> inngåendeHendelserMap, List<HendelsePayload> relevanteHendelser) {
        List<Long> relevanteSekvensnumre = relevanteHendelser.stream()
                .map(HendelsePayload::getSekvensnummer).collect(Collectors.toList());
        for (Map.Entry<Long,InngåendeHendelse> entry : inngåendeHendelserMap.entrySet()) {
            if (!relevanteSekvensnumre.contains(entry.getKey())) {
                InngåendeHendelse inngåendeHendelse = entry.getValue();
                hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
                hendelseRepository.fjernPayload(inngåendeHendelse);
            }
        }
    }

    public List<HendelsePayload> getPayloadsForInngåendeHendelser(List<InngåendeHendelse> inngåendeHendelserListe) {
        List<HendelsePayload> payloadListe = new ArrayList<>();
        if (inngåendeHendelserListe != null) {
            inngåendeHendelserListe.forEach(innHendelse -> payloadListe.add(hendelseTjenesteProvider.finnTjeneste(innHendelse.getType(), innHendelse.getSekvensnummer()).payloadFraString(innHendelse.getPayload())));
        }
        return payloadListe;
    }

    public List<InngåendeHendelse> finnAlleIkkeSorterteHendelserFraFeed(FeedKode feedKode) {
        return hendelseRepository.finnAlleIkkeSorterteHendelserFraFeed(feedKode);
    }
}
