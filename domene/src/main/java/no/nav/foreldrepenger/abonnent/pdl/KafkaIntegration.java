package no.nav.foreldrepenger.abonnent.pdl;

public interface KafkaIntegration {

    /**
     * Er integrasjonen i live.
     *
     * @return true / false
     */
    boolean isAlive();
}
