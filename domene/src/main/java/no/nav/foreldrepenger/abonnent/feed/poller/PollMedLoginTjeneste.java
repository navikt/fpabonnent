package no.nav.foreldrepenger.abonnent.feed.poller;

import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.vedtak.sikkerhet.loginmodule.ContainerLogin;

public class PollMedLoginTjeneste {

    public void poll(FeedPoller feedPoller, InputFeed feed) {
        ContainerLogin containerLogin = new ContainerLogin();
        try {
            containerLogin.login();
            feedPoller.poll(feed);
        } finally {
            containerLogin.logout();
        }
    }
}
