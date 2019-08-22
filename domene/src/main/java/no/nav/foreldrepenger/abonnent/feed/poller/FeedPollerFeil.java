package no.nav.foreldrepenger.abonnent.feed.poller;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface FeedPollerFeil extends DeklarerteFeil {

    FeedPollerFeil FACTORY = FeilFactory.create(FeedPollerFeil.class);

    @TekniskFeil(feilkode = "FP-602051", feilmelding = "Kan ikke utlede nextUrl", logLevel = LogLevel.ERROR)
    Feil kanIkkeUtledeNextUrl(Exception e);

}
