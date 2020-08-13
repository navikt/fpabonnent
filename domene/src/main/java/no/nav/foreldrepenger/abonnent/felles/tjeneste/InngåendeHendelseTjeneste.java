package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;

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
        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(hendelsePayload.getFeedKode(), hendelsePayload.getHendelseId());
        if (hendelse.isPresent()) {
            hendelseRepository.markerHendelseSomSendtNå(hendelse.get());
            hendelseRepository.oppdaterHåndtertStatus(hendelse.get(), HåndtertStatusType.HÅNDTERT);
        }
    }

    public void markerIkkeRelevanteHendelserSomHåndtert(Map<String, InngåendeHendelse> inngåendeHendelserMap, List<HendelsePayload> relevanteHendelser) {
        List<String> relevanteHendelseIder = relevanteHendelser.stream()
                .map(HendelsePayload::getHendelseId).collect(Collectors.toList());
        for (Map.Entry<String,InngåendeHendelse> entry : inngåendeHendelserMap.entrySet()) {
            if (!relevanteHendelseIder.contains(entry.getKey())) {
                InngåendeHendelse inngåendeHendelse = entry.getValue();
                hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
                //TODO(JEJ): Kommentere inn slik at vi fjerner payload når vi har sett at det fungerer (+ bestille patch til null på gamle):
                //hendelseRepository.fjernPayload(inngåendeHendelse);
            }
        }
    }

    public List<HendelsePayload> getPayloadsForInngåendeHendelser(List<InngåendeHendelse> inngåendeHendelserListe) {
        List<HendelsePayload> payloadListe = new ArrayList<>();
        if (inngåendeHendelserListe != null) {
            inngåendeHendelserListe.forEach(innHendelse -> payloadListe.add(hendelseTjenesteProvider.finnTjeneste(innHendelse.getType(), innHendelse.getHendelseId()).payloadFraString(innHendelse.getPayload())));
        }
        return payloadListe;
    }
}
