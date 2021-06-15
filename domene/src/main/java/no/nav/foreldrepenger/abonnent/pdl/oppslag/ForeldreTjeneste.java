package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;

@Dependent
public class ForeldreTjeneste {

    private final FødselTjeneste fødselTjeneste;
    private final AktørTjeneste aktørTjeneste;

    @Inject
    public ForeldreTjeneste(FødselTjeneste fødselTjeneste, AktørTjeneste aktørTjeneste) {
        this.fødselTjeneste = fødselTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    public Set<AktørId> hentForeldre(PersonIdent barn) {
        return fødselTjeneste.hentForeldreTil(barn).stream()
                .flatMap(f -> aktørTjeneste.hentAktørIdForPersonIdent(f).stream())
                .collect(Collectors.toSet());
    }
}
