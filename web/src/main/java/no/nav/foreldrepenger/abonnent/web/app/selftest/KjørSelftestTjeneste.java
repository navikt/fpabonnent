package no.nav.foreldrepenger.abonnent.web.app.selftest;

import no.nav.vedtak.sikkerhet.loginmodule.ContainerLogin;

public class KjørSelftestTjeneste {
    
    public SelftestResultat kjørSelftester(Selftests selftests) {
        SelftestResultat samletResultat;
        ContainerLogin containerLogin = new ContainerLogin();
        try {
            containerLogin.login();  // NOSONAR
            samletResultat = selftests.run();  // NOSONAR
        } finally {
            containerLogin.logout();
        }
        return samletResultat;
    }
}
