package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface HendelseRepositoryFeil extends DeklarerteFeil {

    HendelseRepositoryFeil FACTORY = FeilFactory.create(HendelseRepositoryFeil.class);

    @TekniskFeil(feilkode = "FP-164339", feilmelding = "Fant mer enn en InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", logLevel = LogLevel.WARN)
    Feil fantMerEnnEnHendelseMedStatus(String hendelseKilde, String hendelseId, HåndtertStatusType håndtertStatusType);

    @TekniskFeil(feilkode = "FP-164340", feilmelding = "Fant mer enn en InngåendeHendelse med hendelseId=%s.", logLevel = LogLevel.WARN)
    Feil fantMerEnnEnHendelse(String hendelseId);

    @TekniskFeil(feilkode = "FP-264339", feilmelding = "Fant ikke InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", logLevel = LogLevel.WARN)
    Feil fantIkkeHendelse(String hendelseKilde, String hendelseId, HåndtertStatusType håndtertStatusType);

}
