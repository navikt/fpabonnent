package no.nav.foreldrepenger.abonnent.tps;

import java.util.Set;

public interface PersonTjeneste {

    Set<AktørId> registrerteForeldre(AktørId aktørId);

    boolean erRegistrert(AktørId aktørId);

    boolean harRegistrertDødsdato(AktørId aktørId);
}
