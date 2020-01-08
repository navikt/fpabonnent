package no.nav.foreldrepenger.abonnent.feed.poller;

import java.net.URI;

import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;

public interface FeedPoller {
    FeedKode getFeedKode();

    void poll(InputFeed inputFeed);

    URI request(InputFeed inputFeed);


}
