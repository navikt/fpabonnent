package no.nav.foreldrepenger.abonnent.feed.grovsortering;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.NamedThreadFactory;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.konfig.KonfigVerdi;

/**
 * Håndterer oppstart av tråd som sjekker for hendelser som skal grovsorteres.
 */
@ApplicationScoped
public class GrovsorteringManager implements AppServiceHandler {

    private static final String SORTERING_INTERVALL_SEKUNDER_KEY = "sortering.intervall.sekunder";
    private static final String TRÅDNAVN = "GrovsorteringVurderer-thread";

    private static final Logger log = LoggerFactory.getLogger(GrovsorteringManager.class);

    private GrovsorteringVurderer grovsorteringVurderer;
    private EntityManager entityManager;

    private Integer intervallSekunder;

    /**
     * Future for å kunne kansellere tråden.
     */
    private ScheduledFuture<?> serviceScheduledFuture;
    private ScheduledExecutorService service;

    @Inject
    public GrovsorteringManager(GrovsorteringVurderer grovsorteringVurderer,
                                @VLPersistenceUnit EntityManager entityManager,
                                @KonfigVerdi(SORTERING_INTERVALL_SEKUNDER_KEY) Integer intervallSekunder) {
        this.grovsorteringVurderer = grovsorteringVurderer;
        this.entityManager = entityManager;
        this.intervallSekunder = intervallSekunder;
    }

    @Override
    public synchronized void start() {
        if (serviceScheduledFuture != null) {
            throw new IllegalStateException("Grovsortering allerede startet, stopp først"); //$NON-NLS-1$
        }
        if (service == null) {
            service = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(TRÅDNAVN));
            log.info("Opprettet executor thread for grovsortering {}", TRÅDNAVN); // NOSONAR
        }
        GrovsorteringTråd vurderer = new GrovsorteringTråd(grovsorteringVurderer, entityManager);
        serviceScheduledFuture = service.scheduleWithFixedDelay(vurderer, intervallSekunder / 2, intervallSekunder, TimeUnit.SECONDS); // NOSONAR
        log.debug("Skedulert grovsorteringstråder med intervall={}s", intervallSekunder);
    }

    @Override
    public synchronized void stop() {
        if (serviceScheduledFuture != null) {
            serviceScheduledFuture.cancel(true);
            serviceScheduledFuture = null;
        }
    }
}
