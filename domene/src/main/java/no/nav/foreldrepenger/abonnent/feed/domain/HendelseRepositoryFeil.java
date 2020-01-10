package no.nav.foreldrepenger.abonnent.feed.domain;

import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface HendelseRepositoryFeil extends DeklarerteFeil {

    HendelseRepositoryFeil FACTORY = FeilFactory.create(HendelseRepositoryFeil.class);

    @TekniskFeil(feilkode = "FP-164339", feilmelding = "Fant mer enn en InngåendeHendelse med feedKode=%s, sekvensnummer=%s og håndtertStatus=%s.", logLevel = LogLevel.WARN)
    Feil fantMerEnnEnHendelse(String feedKode, Long sekvensnummer, HåndtertStatusType håndtertStatusType);

    @TekniskFeil(feilkode = "FP-264339", feilmelding = "Fant ikke InngåendeHendelse med feedKode=%s, sekvensnummer=%s og håndtertStatus=%s.", logLevel = LogLevel.WARN)
    Feil fantIkkeHendelse(String feedKode, Long sekvensnummer, HåndtertStatusType håndtertStatusType);

}
