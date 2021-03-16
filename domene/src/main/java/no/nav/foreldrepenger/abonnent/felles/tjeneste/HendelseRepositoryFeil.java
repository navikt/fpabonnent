package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.vedtak.exception.TekniskException;

public class HendelseRepositoryFeil {

    public static TekniskException fantMerEnnEnHendelseMedStatus(String hendelseKilde, String hendelseId, HåndtertStatusType håndtertStatusType) {
        return new TekniskException("FP-164339", String.format("Fant mer enn en InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", hendelseKilde, hendelseId, håndtertStatusType));
    }

    public static TekniskException fantMerEnnEnHendelse(String hendelseId) {
        return new TekniskException("FP-164340", String.format("Fant mer enn en InngåendeHendelse med hendelseId=%s.", hendelseId));
    }

    public static TekniskException fantIkkeHendelse(String hendelseKilde, String hendelseId, HåndtertStatusType håndtertStatusType) {
        return new TekniskException("FP-264339", String.format("Fant ikke InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", hendelseKilde, hendelseId, håndtertStatusType));
    }

}
