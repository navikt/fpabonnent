package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

public class AbonnentHendelserFeil {

    private AbonnentHendelserFeil() {
    }

    public static TekniskException prosesstaskPreconditionManglerProperty(TaskType taskname, String property, Long taskId) {
        return new TekniskException("FP-690327",
            String.format("Prosessering av preconditions for %s mangler %s. TaskId: %s", taskname, property, taskId));
    }

    public static TekniskException ukjentMeldingtypeKanIkkeFinneHendelseTjeneste(String hendelseType, String hendelseId) {
        return new TekniskException("FP-309345",
            String.format("Ukjent hendelsestype - kan ikke finne hendelsestjeneste. hendelseType=%s, hendelseId=%s", hendelseType, hendelseId));
    }

    public static TekniskException merEnnEnHendelseTjenesteFunnet(String hendelseType, String hendelseId) {
        return new TekniskException("FP-125639",
            String.format("Mer enn en hendelsestjeneste funnet. hendelseType=%s, hendelseId=%s", hendelseType, hendelseId));
    }

    public static TekniskException ukjentHendelseType() {
        return new TekniskException("FP-846675", "Ukjent hendelsestype");
    }

    public static TekniskException finnerIngenAktørId(String hendelseId) {
        return new TekniskException("FP-195374", String.format("Finner ingen aktørId på hendelseId=%s", hendelseId));
    }

    public static TekniskException merEnnEnAktørId(int antall, String hendelseId) {
        return new TekniskException("FP-295374", String.format("Finner ikke unik aktørId. Fant %s aktørId på hendelseId=%s", antall, hendelseId));
    }

    public static TekniskException manglerInngåendeHendelseIdPåProsesstask(TaskType prosesstaskType, Long taskId) {
        return new TekniskException("FP-144656",
            String.format("InngåendeHendelse ID mangler på %s med TaskId=%s", prosesstaskType, taskId));
    }
}
