package no.nav.foreldrepenger.abonnent.pdl.oppslag;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;

@ApplicationScoped
public class ForeldreTjeneste {

    private FødselTjeneste fødselTjeneste;
    private AktørTjeneste aktørTjeneste;

    ForeldreTjeneste() {
        //CDI
    }

    @Inject
    public ForeldreTjeneste(FødselTjeneste fødselTjeneste, AktørTjeneste aktørTjeneste) {
        this.fødselTjeneste = fødselTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    public Set<AktørId> hentForeldre(PersonIdent barn) {
        return fødselTjeneste.hentForeldreTil(barn)
            .stream()
            .flatMap(f -> aktørTjeneste.hentAktørIdForPersonIdent(f).stream())
            .collect(Collectors.toSet());
    }

    public Optional<LocalDate> hentFødselsdato(PersonIdent barn) {
        return fødselTjeneste.hentFødselsdato(barn);
    }
}
