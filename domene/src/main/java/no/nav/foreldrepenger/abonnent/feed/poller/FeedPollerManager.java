package no.nav.foreldrepenger.abonnent.feed.poller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.NamedThreadFactory;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.pdl.PdlFeatureToggleTjeneste;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.util.Tuple;

/**
 * Main class handling polling JSON feed.
 */
@ApplicationScoped
public class FeedPollerManager implements AppServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedPollerManager.class);

    private FeedPollerRepositoryImpl feedPollerRepository;

    private PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste;

    /**
     * Prefix every thread in pool with given name.
     */
    private final String threadPoolNamePrefix = getClass().getSimpleName();

    /**
     * Delay between each interval of polling. (millis)
     */
    private long delayBetweenPollingMillis = getSystemPropertyWithLowerBoundry("task.manager.polling.delay", 500L, 50L);


    /**
     * Single scheduled thread handling polling.
     */
    private Map<FeedKode, Tuple<FeedPoller, ScheduledExecutorService>> pollingService;

    /**
     * Future for å kunne kansellere polling.
     */
    private Collection<ScheduledFuture<?>> pollingServiceScheduledFuture;
    private Instance<FeedPoller> feedPollers;

    public FeedPollerManager() {
    }

    @Inject
    public FeedPollerManager(FeedPollerRepositoryImpl feedPollerRepository, @Any Instance<FeedPoller> feedPollers, PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste) {
        Objects.requireNonNull(feedPollerRepository, "feedPollerRepository"); //$NON-NLS-1$
        Objects.requireNonNull(feedPollers, "feedPollers"); //$NON-NLS-1$
        this.feedPollerRepository = feedPollerRepository;
        this.feedPollers = feedPollers;
        this.pdlFeatureToggleTjeneste = pdlFeatureToggleTjeneste;
    }

    @Override
    public synchronized void start() {
        if (pdlFeatureToggleTjeneste.skalKonsumerePf()) {
            startPollerThread();
        } else {
            log.info("Person-feed er deaktivert i dette clusteret");
        }
    }

    @Override
    public synchronized void stop() {
        if (pollingServiceScheduledFuture != null) {
            for (ScheduledFuture<?> scheduledFuture : pollingServiceScheduledFuture) {
                scheduledFuture.cancel(true);
            }
            pollingServiceScheduledFuture = null;
        }
    }

    synchronized void startPollerThread() {
        if (pollingServiceScheduledFuture != null) {
            throw new IllegalStateException("Service allerede startet, stopp først");//$NON-NLS-1$
        }
        if (pollingService == null) {
            this.pollingService = new LinkedHashMap<>();
            for (FeedPoller feedPoller : feedPollers) {
                String threadName = threadPoolNamePrefix + "-" + feedPoller.getFeedKode().getKode() + "-poller";
                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(threadName));
                Tuple<FeedPoller, ScheduledExecutorService> tuple = new Tuple<>(feedPoller, service);
                pollingService.put(feedPoller.getFeedKode(), tuple);
                log.info("Created thread for JSON feed polling {}", threadName); // NOSONAR
            }
        }
        this.pollingServiceScheduledFuture = new ArrayList<>();
        for (Map.Entry<FeedKode, Tuple<FeedPoller, ScheduledExecutorService>> tupleEntry : pollingService.entrySet()) {
            Tuple<FeedPoller, ScheduledExecutorService> tuple = tupleEntry.getValue();
            FeedPoller feedPoller = tuple.getElement1();
            Poller poller = new Poller(feedPoller, feedPollerRepository);
            ScheduledExecutorService service = tuple.getElement2();
            ScheduledFuture<?> scheduledFuture = service.scheduleWithFixedDelay(poller, delayBetweenPollingMillis * 2, delayBetweenPollingMillis, TimeUnit.MILLISECONDS);// NOSONAR
            pollingServiceScheduledFuture.add(scheduledFuture);
            log.debug("Lagt til ny poller til pollingtjeneste. poller={}, delayBetweenPollingMillis={}", feedPoller.getFeedKode().getKode(), delayBetweenPollingMillis);
        }
    }

    private static long getSystemPropertyWithLowerBoundry(String key, long defaultValue, long lowerBoundry) {
        final String property = System.getProperty(key, String.valueOf(defaultValue));
        final long systemPropertyValue = Long.parseLong(property);
        if (systemPropertyValue < lowerBoundry) {
            return lowerBoundry;
        }
        return systemPropertyValue;
    }
}
