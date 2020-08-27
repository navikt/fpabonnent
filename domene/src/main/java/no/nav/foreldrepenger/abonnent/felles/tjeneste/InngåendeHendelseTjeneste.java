package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.util.Objects;
import java.util.Optional;

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

    public Optional<InngåendeHendelse> finnHendelseSomErSendtTilSortering(String hendelseId) {
        Objects.requireNonNull(hendelseId, "mangler hendelseId for inngående hendelse");  //$NON-NLS-1$
        return hendelseRepository.finnHendelseSomErSendtTilSortering(hendelseId);
    }

    public void oppdaterHåndtertStatus(InngåendeHendelse inngåendeHendelse, HåndtertStatusType håndtertStatusType) {
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, håndtertStatusType);
    }

    public void oppdaterHendelseSomSendtNå(HendelsePayload hendelsePayload) {
        Optional<InngåendeHendelse> hendelse = hendelseRepository.finnGrovsortertHendelse(hendelsePayload.getHendelseKilde(), hendelsePayload.getHendelseId());
        if (hendelse.isPresent()) {
            hendelseRepository.markerHendelseSomSendtNå(hendelse.get());
            hendelseRepository.oppdaterHåndtertStatus(hendelse.get(), HåndtertStatusType.HÅNDTERT);
        }
    }

    public void markerHendelseSomHåndtertOgFjernPayload(InngåendeHendelse inngåendeHendelse) {
        hendelseRepository.oppdaterHåndtertStatus(inngåendeHendelse, HåndtertStatusType.HÅNDTERT);
        //TODO(TFP-3680): Kommentere inn slik at vi fjerner payload når vi har sett at det fungerer (+ bestille patch til null på gamle):
        //hendelseRepository.fjernPayload(inngåendeHendelse);
    }

    public HendelsePayload hentUtPayloadFraInngåendeHendelse(InngåendeHendelse inngåendeHendelse) {
        return hendelseTjenesteProvider.finnTjeneste(inngåendeHendelse.getHendelseType(), inngåendeHendelse.getHendelseId())
                .payloadFraJsonString(inngåendeHendelse.getPayload());
    }
}
