package no.nav.foreldrepenger.abonnent.pdl;

import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedfoedtbarn.DoedfoedtBarn;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.foedsel.Foedsel;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseHåndterer.class);

    private ProsessTaskRepository taskRepository;

    PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    void handleMessage(String key, Personhendelse payload) {
        setCallIdForHendelse(payload);

        String opplysningstype = payload.getOpplysningstype().toString();
        String hendelseId = payload.getHendelseId().toString();
        String endringstype = payload.getEndringstype().toString();
        String personidenter = payload.getPersonidenter().stream().map(CharSequence::toString).collect(Collectors.joining(","));

        LOG.info("FPABONNENT mottok Personhendelse: key={} hendelseId={} opplysningstype={} endringstype={} personidenter={} master={} opprettet={} tidligereHendelseId={}",
                key, hendelseId, opplysningstype, endringstype, personidenter, payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        Foedsel foedsel = payload.getFoedsel();
        if (foedsel != null) {
            LOG.info("Fødsel: fødselsdato={} fødselsår={} fødested={} fødeKommune={} fødeland={}", foedsel.getFoedselsdato(), foedsel.getFoedselsaar(), foedsel.getFoedested(), foedsel.getFoedekommune(), foedsel.getFoedeland());
        }
        Doedsfall doedsfall = payload.getDoedsfall();
        if (doedsfall != null) {
            LOG.info("Dødsfall: dødsdato={}", doedsfall.getDoedsdato());
        }
        DoedfoedtBarn doedfoedtBarn = payload.getDoedfoedtBarn();
        if (doedfoedtBarn != null) {
            LOG.info("DødfødtBarn: dato={}", doedfoedtBarn.getDato());
        }

        //TODO(JEJ): Opprette prosesstask for videre håndtering
    }

    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId(UUID.randomUUID().toString());
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }
}
