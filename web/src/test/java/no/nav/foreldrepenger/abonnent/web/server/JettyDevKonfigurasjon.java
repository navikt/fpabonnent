package no.nav.foreldrepenger.abonnent.web.server;

public class JettyDevKonfigurasjon extends JettyWebKonfigurasjon {
    private static final int DEV_SERVER_PORT = 8065;
    private static final int SSL_SERVER_PORT = 8059;

    public JettyDevKonfigurasjon() {
        super(DEV_SERVER_PORT);
    }

    @Override
    public int getSslPort() {
        return SSL_SERVER_PORT;
    }

}
