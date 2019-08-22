package no.nav.foreldrepenger.abonnent.feed.grovsortering;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface GrovsorteringFeil extends DeklarerteFeil {

    GrovsorteringFeil FACTORY = FeilFactory.create(GrovsorteringFeil.class);

    @TekniskFeil(feilkode = "FP-962343", feilmelding = "Noe gikk galt ved utføring av grovsortering i transaksjon", logLevel = LogLevel.ERROR)
    Feil kanIkkeUtføreGrovsorteringITransaksjon(Exception e);

    @TekniskFeil(feilkode = "FP-462049", feilmelding = "Noe gikk galt når hendelser skulle sendes til grovsortering", logLevel = LogLevel.ERROR)
    Feil kanIkkeSjekkeEtterHendelserTilGrovsortering(Exception e);

    @TekniskFeil(feilkode = "FP-137303", feilmelding = "Uventet feil ved grovsortering", logLevel = LogLevel.ERROR)
    Feil uventetFeilVedGrovsortering(Exception e);
}
