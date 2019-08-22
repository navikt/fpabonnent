package no.nav.foreldrepenger.abonnent.feed.grovsortering;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.RequestContextHandler;
import no.nav.vedtak.felles.jpa.TransactionHandler;

/**
 * Tråden sørger for at grovsorteringen kjører i egen transaksjon med request scope aktivt.
 */
public class GrovsorteringTråd implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(GrovsorteringTråd.class);

    private GrovsorteringVurderer grovsorteringVurderer;
    private EntityManager entityManager;

    public GrovsorteringTråd(GrovsorteringVurderer grovsorteringVurderer, EntityManager entityManager) {
        this.grovsorteringVurderer = grovsorteringVurderer;
        this.entityManager = entityManager;
    }

    @Override
    public void run() {
        try {
            RequestContextHandler.doWithRequestContext(this::utførGrovsorteringITransaksjon);
        } catch (Exception e) {
            GrovsorteringFeil.FACTORY.uventetFeilVedGrovsortering(e).log(log);
        }
    }

    private Void utførGrovsorteringITransaksjon() {
        try {
            new GrovsorteringITransaksjon().doWork();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) { // NOSONAR
            GrovsorteringFeil.FACTORY.kanIkkeUtføreGrovsorteringITransaksjon(e).log(log);
        }
        return null;
    }

    private final class GrovsorteringITransaksjon extends TransactionHandler<Void> {

        Void doWork() throws Exception {
            try {
                return super.apply(entityManager);
            } finally {
                CDI.current().destroy(entityManager);
            }
        }

        @Override
        protected Void doWork(EntityManager entityManager) {
            try {
                grovsorteringVurderer.vurderGrovsortering();
            } finally {
                CDI.current().destroy(entityManager);
            }
            return null;
        }
    }
}
