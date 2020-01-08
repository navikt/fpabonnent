package no.nav.foreldrepenger.abonnent.felles;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelsePayload;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;

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
    public <T extends HendelsePayload> HendelseTjeneste<T> finnTjeneste(HendelseType hendelseType, Long sekvensnummer) {
        Instance<HendelseTjeneste<? extends HendelsePayload>> selected = tjenester.select(new HendelseTypeRef.HendelseTypeRefLiteral(hendelseType));

        if (selected.isAmbiguous()) {
            throw AbonnentHendelserFeil.FACTORY.merEnnEnHendelseTjenesteFunnet(hendelseType.getKode(), sekvensnummer).toException();
        } else if (selected.isUnsatisfied()) {
            throw AbonnentHendelserFeil.FACTORY.ukjentMeldingtypeKanIkkeFinneHendelseTjeneste(hendelseType.getKode(), sekvensnummer).toException();
        }
        HendelseTjeneste<? extends HendelsePayload> minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (HendelseTjeneste<T>) minInstans;
    }
}
