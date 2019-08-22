package no.nav.foreldrepenger.abonnent.feed.poller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.enterprise.inject.Instance;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.qos.logback.classic.Level;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.felles.FeedKode;
import no.nav.modig.core.test.LogSniffer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FeedPollerManagerTest {

    @Rule
    public LogSniffer logSniffer = new LogSniffer(Level.ALL);

    private FeedPollerManager manager;

    @Before
    public void setUp() {
        FeedPollerRepositoryImpl feedPollerRepositoryImpl = mock(FeedPollerRepositoryImpl.class);
        Instance<FeedPoller> feedPollers = mock(Instance.class);
        Iterator<FeedPoller> iterator = mock(Iterator.class);

        when(feedPollers.get()).thenReturn(new TestFeedPoller());
        when(feedPollers.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(new TestFeedPoller());
        manager = new FeedPollerManager(feedPollerRepositoryImpl, feedPollers);
    }

    @Test
    public void skal_legge_til_poller() {
        manager.start();
        logSniffer.assertHasInfoMessage("Created thread for JSON feed polling FeedPollerManager-JF_TPS-poller");
        Assertions.assertThat(logSniffer.countEntries("Lagt til ny poller til pollingtjeneste. poller=JF_TPS, delayBetweenPollingMillis=500")).isEqualTo(1);
    }

    private class TestFeedPoller implements FeedPoller {

        @Override
        public FeedKode getFeedKode() {
            return FeedKode.TPS;
        }

        @Override
        public void poll(InputFeed inputFeed) {
            System.out.println("FOO");
        }

        @Override
        public URI request(InputFeed inputFeed) {
            try {
                return new URI("https//foo.bar");
            } catch (URISyntaxException e) {
                return null;
            }
        }

    }


}
