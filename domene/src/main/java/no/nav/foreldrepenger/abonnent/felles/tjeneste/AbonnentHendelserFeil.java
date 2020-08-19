package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface AbonnentHendelserFeil extends DeklarerteFeil {

    AbonnentHendelserFeil FACTORY = FeilFactory.create(AbonnentHendelserFeil.class);

    @TekniskFeil(feilkode = "FP-690327", feilmelding = "Prosessering av preconditions for %s mangler %s. TaskId: %s", logLevel = WARN)
    Feil prosesstaskPreconditionManglerProperty(String taskname, String property, Long taskId);
    
    @TekniskFeil(feilkode = "FP-309345", feilmelding = "Ukjent hendelsestype - kan ikke finne hendelsestjeneste. hendelseType=%s, hendelseId=%s", logLevel = LogLevel.ERROR)
    Feil ukjentMeldingtypeKanIkkeFinneHendelseTjeneste(String hendelseType, String hendelseId);

    @TekniskFeil(feilkode = "FP-125639", feilmelding = "Mer enn en hendelsestjeneste funnet. hendelseType=%s, hendelseId=%s", logLevel = LogLevel.ERROR)
    Feil merEnnEnHendelseTjenesteFunnet(String hendelseType, String hendelseId);

    @TekniskFeil(feilkode = "FP-846675", feilmelding = "Ukjent Hendelse Type <%s>", logLevel = LogLevel.WARN)
    Feil ukjentHendelseType(String hendelseType);

    @TekniskFeil(feilkode = "FP-195374", feilmelding = "Finner ingen aktørId på hendelseId=%s", logLevel = LogLevel.WARN)
    Feil finnerIngenAktørId(String hendelseId);

    @TekniskFeil(feilkode = "FP-295374", feilmelding = "Finner ikke unik aktørId. Fant %s aktørId på hendelseId=%s", logLevel = LogLevel.INFO)
    Feil merEnnEnAktørId(int antall, String hendelseId);

    @TekniskFeil(feilkode = "FP-144656", feilmelding = "InngåendeHendelse ID mangler på prosess task %s med TaskId=%s", logLevel = LogLevel.WARN)
    Feil manglerInngåendeHendelseIdPåProsesstask(String prosesstaskType, Long taskId);
}
