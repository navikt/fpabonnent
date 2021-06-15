package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType.HÅNDTERT;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;

@ApplicationScoped
public class InngåendeHendelseTjeneste {

    private HendelseRepository repo;
    private HendelseTjenesteProvider hendelseTjenesteProvider;

    InngåendeHendelseTjeneste() {
        // CDI
    }

    @Inject
    public InngåendeHendelseTjeneste(HendelseRepository repo, HendelseTjenesteProvider hendelseTjenesteProvider) {
        this.repo = repo;
        this.hendelseTjenesteProvider = hendelseTjenesteProvider;
    }

    public Optional<InngåendeHendelse> finnHendelseSomErSendtTilSortering(String hendelseId) {
        Objects.requireNonNull(hendelseId, "mangler hendelseId for inngående hendelse");
        return repo.finnHendelseSomErSendtTilSortering(hendelseId);
    }

    public void oppdaterHåndtertStatus(InngåendeHendelse h, HåndtertStatusType type) {
        repo.oppdaterHåndtertStatus(h, type);
    }

    public void oppdaterHendelseSomSendtNå(HendelsePayload hendelsePayload) {
        repo.finnGrovsortertHendelse(hendelsePayload.getHendelseKilde(), hendelsePayload.getHendelseId()).ifPresent(h -> {
            repo.markerHendelseSomSendtNå(h);
            repo.oppdaterHåndtertStatus(h, HÅNDTERT);
        });
    }

    public void markerHendelseSomHåndtertOgFjernPayload(InngåendeHendelse h) {
        repo.oppdaterHåndtertStatus(h, HÅNDTERT);
        repo.fjernPayload(h);
    }

    public HendelsePayload hentUtPayloadFraInngåendeHendelse(InngåendeHendelse h) {
        return hendelseTjenesteProvider.finnTjeneste(h.getHendelseType(), h.getHendelseId()).payloadFraJsonString(h.getPayload());
    }
}
