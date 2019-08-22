package no.nav.foreldrepenger.abonnent.feed.poller;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;

import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.felles.RequestContextHandler;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.jpa.TransactionHandler;
import no.nav.vedtak.log.mdc.MDCOperations;

public class Poller implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Poller.class);
    
    
    private FeedPoller feedPoller;
    private FeedPollerRepositoryImpl repository;
    private PollMedLoginTjeneste pollMedLoginTjeneste;

    /**
     * simple backoff interval in seconds per round to account for transient database errors.
     */
    private static final int[] BACKOFF_INTERVALL_IN_SEC = new int[]{1, 2, 5, 5, 10, 10, 10, 10, 30};
    private final AtomicInteger backoffRound = new AtomicInteger();

    public Poller(FeedPoller feedPoller, FeedPollerRepositoryImpl repository) {
        this.feedPoller = feedPoller;
        this.repository = repository;
        this.pollMedLoginTjeneste = new PollMedLoginTjeneste();
    }
    
    Poller(FeedPoller feedPoller, FeedPollerRepositoryImpl repository, PollMedLoginTjeneste pollMedLoginTjeneste) {
        this.feedPoller = feedPoller;
        this.repository = repository;
        this.pollMedLoginTjeneste = pollMedLoginTjeneste;
    } 

    @Override
    public void run() {
        try {
            RequestContextHandler.doWithRequestContext(this::doPollingWithEntityManager);
        } catch (Exception e) {
            logger.error("Uventet feil", e);
        }
    }

    private final class PollInNewTransaction extends TransactionHandler<Void> {

        Void doWork() throws Exception {

            EntityManager entityManager = repository.getEntityManager();
            try {
                return super.apply(entityManager);
            } finally {
                CDI.current().destroy(entityManager);
            }
        }
        
        private Void doPoll() {
            EntityManager entityManager = repository.getEntityManager();

            try {
                Optional<InputFeed> feed = repository.finnInputFeed(feedPoller.getFeedKode());
                if (!feed.isPresent()) {
                    logger.debug("{} kjøres av en annen node", feedPoller.getFeedKode().getKode()); //NOSONAR
                    return null;
                }
                InputFeed feedKonfig = feed.get();
                if (PollerUtils.klarTilÅKjøres(feedKonfig)) {
                    poll(feedKonfig);
                }
                entityManager.persist(feedKonfig);
                entityManager.flush();
                return null;
            } finally {
                CDI.current().destroy(entityManager);
            }
        }

        @Override
        protected Void doWork(EntityManager entityManager) {
            return doPoll();
        }
        
        private void poll(InputFeed feed) {
            try {
                MDCOperations.putCallId();
                pollMedLoginTjeneste.poll(feedPoller, feed);
            } catch (VLException e) { // NOSONAR
                feed.oppdaterFeilet();
                e.log(logger);
            } catch (Exception e) {
                feed.oppdaterFeilet();
                Feilene.FACTORY.uventetFeilVedLesing(feed.getKode().getKode(), e).log(logger);
            } finally {
                MDCOperations.removeCallId();
            }
        }
    }

    private Void doPollingWithEntityManager() {
        try {
            if (backoffRound.get() > 0) {
                Thread.sleep(BACKOFF_INTERVALL_IN_SEC[Math.min(backoffRound.get(), BACKOFF_INTERVALL_IN_SEC.length) - 1] * 1000L);
            }
            new PollInNewTransaction().doWork();
            
            backoffRound.set(0);

            return null;
        } catch (InterruptedException e) {
            backoffRound.incrementAndGet();
            Thread.currentThread().interrupt();
        } catch (JDBCConnectionException e) { // NOSONAR
            backoffRound.incrementAndGet();
            Feilene.FACTORY.midlertidigUtilgjengeligDatabase(backoffRound.get(), e.getClass(), e.getMessage())
                    .log(logger);
        } catch (Exception e) { // NOSONAR
            backoffRound.incrementAndGet();
            Feilene.FACTORY.kunneIkkePolleDatabase(backoffRound.get(), e.getClass(), e.getMessage(), e)
                    .log(logger);
        }
        return null;
    }

    interface Feilene extends DeklarerteFeil {
        Feilene FACTORY = FeilFactory.create(Feilene.class);

        @TekniskFeil(feilkode = "FP-846674", feilmelding = "Transient datase connection feil, venter til neste runde (runde=%s): %s: %s", logLevel = LogLevel.WARN)
        Feil midlertidigUtilgjengeligDatabase(Integer round, Class<?> exceptionClass, String exceptionMessage);

        @TekniskFeil(feilkode = "FP-142862", feilmelding = "Kunne ikke polle database, venter til neste runde(runde=%s): %s: %s", logLevel = LogLevel.WARN)
        Feil kunneIkkePolleDatabase(Integer round, Class<?> exceptionClass, String exceptionMessage, Exception cause);

        @TekniskFeil(feilkode = "FP-573550", feilmelding = "Uventet feil ved lesing av JSON feed '%s'", logLevel = LogLevel.WARN)
        Feil uventetFeilVedLesing(String feedKode, Exception cause);
    }    
}
