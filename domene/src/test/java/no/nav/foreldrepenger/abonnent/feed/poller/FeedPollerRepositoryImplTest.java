package no.nav.foreldrepenger.abonnent.feed.poller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;

public class FeedPollerRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    FeedPollerRepositoryImpl feedPollerRepository = new FeedPollerRepositoryImpl(repoRule.getEntityManager());

    @Test
    public void skal_ikke_finne_InputFeed() {
        assertThat(feedPollerRepository.finnInputFeed(FeedKode.UDEFINERT)).isNotPresent();
    }

    @Test
    public void skal_finne_lagret_input_feed() {
        InputFeed inputFeed = InputFeed.builder()
                .feiletAntall(1)
                .kode(FeedKode.UDEFINERT)
                .navn("navn")
                .nextUrl("nextUrl")
                .sistFeilet(LocalDateTime.now())
                .sistLest(LocalDateTime.now())
                .ventetidFeilet("ventetidFeilet")
                .ventetidFerdiglest("ventetidFerdiglest")
                .ventetidLesbar("ventetidLesbar")
                .build();
        feedPollerRepository.getEntityManager().persist(inputFeed);
        feedPollerRepository.getEntityManager().flush();

        Optional<InputFeed> finnInputFeed = feedPollerRepository.finnInputFeed(FeedKode.UDEFINERT);

        assertThat(finnInputFeed).isPresent();
    }

}
