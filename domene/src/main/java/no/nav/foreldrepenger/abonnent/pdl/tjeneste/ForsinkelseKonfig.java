package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.konfig.Environment;

/**
 * Vi ønsker ikke forsinkelser når applikasjonen kjører lokalt, både ved manuell
 * testing og automatisert igjennom Autotest.
 */
@ApplicationScoped
public class ForsinkelseKonfig {
    private static final Environment ENV = Environment.current();

    public boolean skalForsinkeHendelser() {
        return !ENV.isLocal();
    }

    public int normalForsinkelseMinutter() {
        return ENV.isProd() ? 24 * 60 : 5;
    }
}
