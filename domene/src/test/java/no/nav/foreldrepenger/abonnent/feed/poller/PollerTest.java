package no.nav.foreldrepenger.abonnent.feed.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
//import no.nav.modig.core.test.LogSniffer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class PollerTest {
    // @Rule
    // public LogSniffer logSniffer = new LogSniffer(Level.ALL);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private FeedPollerRepositoryImpl feedPollerRepository = new FeedPollerRepositoryImpl(repoRule.getEntityManager());

    private Poller poller;
    private FeedPoller feedPoller;
    private InputFeed inputFeed;
    private PollMedLoginTjeneste pollMedLoginTjeneste;

    @Before
    public void setUp() {
        pollMedLoginTjeneste = mock(PollMedLoginTjeneste.class);
        feedPoller = mock(FeedPoller.class);
        when(feedPoller.getFeedKode()).thenReturn(FeedKode.UDEFINERT);

        inputFeed = InputFeed.builder().kode(FeedKode.UDEFINERT).navn("Unit test poller").ventetidFeilet("PT1S")
                .ventetidFerdiglest("PT1S").ventetidLesbar("PT1S").feiletAntall(0).build();
        repoRule.getEntityManager().persist(inputFeed);
        repoRule.getEntityManager().flush();
        System.setProperty("loadbalancer.url", "http://test");

        poller = new Poller(feedPoller, feedPollerRepository, pollMedLoginTjeneste);
    }

    @After
    public void tearDown() {
        // logSniffer.clearLog();
    }

    @Test
    public void skal_logge_exception_ved_feil_ved_polling() {
        Poller pollerSomFårNPE = new Poller(null, null);
        pollerSomFårNPE.run();
        // logSniffer.assertHasWarnMessage(
        // "FP-142862:Kunne ikke polle database, venter til neste runde(runde=1): class
        // java.lang.NullPointerException: null");
    }

    @Test
    public void skal_kjøre_test_feed_poller() {
        poller.run();

        assertThat(inputFeed.getFeiletAntall()).isEqualTo(0);
    }

    @Test
    public void skal_behandle_VLEXception() {
        doThrow(AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent("type", 1).toException())
                .when(pollMedLoginTjeneste).poll(feedPoller, inputFeed);
        poller.run();

        assertThat(inputFeed.getFeiletAntall()).isEqualTo(1);
        // logSniffer.assertHasErrorMessage("FP-730005");
    }

    @Test
    public void skal_behandle_ukjent_feil() {
        doThrow(new RuntimeException()).when(pollMedLoginTjeneste).poll(feedPoller, inputFeed);
        poller.run();

        assertThat(inputFeed.getFeiletAntall()).isEqualTo(1);
        // logSniffer.assertHasWarnMessage("Uventet feil ved lesing av JSON feed");
    }

}
