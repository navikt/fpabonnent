package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import java.util.Collection;

public class Valideringsfeil extends RuntimeException {
    private final Collection<FeltFeilDto> feltfeil;

    public Valideringsfeil(Collection<FeltFeilDto> feltfeil) {
        this.feltfeil = feltfeil;
    }

    public Collection<FeltFeilDto> getFeltfeil() {
        return feltfeil;
    }

}
