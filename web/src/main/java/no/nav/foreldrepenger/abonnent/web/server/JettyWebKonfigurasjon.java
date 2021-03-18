package no.nav.foreldrepenger.abonnent.web.server;

public class JettyWebKonfigurasjon {

    public static final String CONTEXT_PATH = "/fpabonnent";

    private final Integer serverPort;

    public JettyWebKonfigurasjon(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getContextPath() {
        return CONTEXT_PATH;
    }
}
