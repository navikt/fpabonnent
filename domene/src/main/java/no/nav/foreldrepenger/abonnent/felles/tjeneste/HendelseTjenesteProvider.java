package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

@ApplicationScoped
public class HendelseTjenesteProvider {

    private Instance<HendelseTjeneste<? extends HendelsePayload>> tjenester;

    HendelseTjenesteProvider() {
        // CDI
    }

    @Inject
    public HendelseTjenesteProvider(@Any Instance<HendelseTjeneste<? extends HendelsePayload>> tjenester) {
        this.tjenester = tjenester;
    }

    @SuppressWarnings("unchecked")
    public <T extends HendelsePayload> HendelseTjeneste<T> finnTjeneste(HendelseType hendelseType, String hendelseId) {
        Instance<HendelseTjeneste<? extends HendelsePayload>> selected = tjenester.select(new HendelseTypeRef.HendelseTypeRefLiteral(hendelseType));

        if (selected.isAmbiguous()) {
            throw AbonnentHendelserFeil.merEnnEnHendelseTjenesteFunnet(hendelseType.getKode(), hendelseId);
        } else if (selected.isUnsatisfied()) {
            throw AbonnentHendelserFeil.ukjentMeldingtypeKanIkkeFinneHendelseTjeneste(hendelseType.getKode(), hendelseId);
        }
        HendelseTjeneste<? extends HendelsePayload> minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (HendelseTjeneste<T>) minInstans;
    }
}
