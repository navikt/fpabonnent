package no.nav.foreldrepenger.abonnent.web.server;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

public class JettyWebKonfigurasjon {

    public static final String CONTEXT_PATH = "/fpabonnent";

    private final Integer serverPort;

    public JettyWebKonfigurasjon(int serverPort) {
        this.serverPort = serverPort;
        ContextPathHolder.instance(CONTEXT_PATH);
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getContextPath() {
        return CONTEXT_PATH;
    }
}
