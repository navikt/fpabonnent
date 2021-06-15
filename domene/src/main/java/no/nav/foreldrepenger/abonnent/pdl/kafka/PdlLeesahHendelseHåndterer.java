package no.nav.foreldrepenger.abonnent.pdl.kafka;

import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.DØD;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.DØDFØDSEL;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.FØDSEL;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlPersonhendelse;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseHåndterer.class);

    private HendelseRepository hendelseRepository;
    private PdlLeesahOversetter oversetter;
    private ProsessTaskRepository prosessTaskRepository;
    private ForsinkelseTjeneste forsinkelseTjeneste;

    PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(HendelseRepository hendelseRepository,
            PdlLeesahOversetter pdlLeesahOversetter,
            ProsessTaskRepository prosessTaskRepository,
            ForsinkelseTjeneste forsinkelseTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.oversetter = pdlLeesahOversetter;
        this.prosessTaskRepository = prosessTaskRepository;
        this.forsinkelseTjeneste = forsinkelseTjeneste;
    }

    void handleMessage(String key, Personhendelse payload) { // key er spesialtegn + aktørId, som også finnes i payload
        setCallIdForHendelse(payload);

        Optional<InngåendeHendelse> inngåendeHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(payload.getHendelseId().toString(),
                HendelseKilde.PDL);
        if (inngåendeHendelse.isPresent()) {
            LOG.warn(
                    "FPABONNENT mottok duplikat hendelse som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}. Tiltak: Sjekk om det skjedde en deploy/restart av Fpabonnent i det samme tidsrommet - i så fall kan dette ignoreres.",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId());
            return;
        }

        if (FØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterFødsel(payload);
        } else if (DØD.contentEquals(payload.getOpplysningstype())) {
            håndterDødsfall(payload);
        } else if (DØDFØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterDødfødtBarn(payload);
        } else {
            LOG.info(
                    "FPABONNENT mottok en ukjent hendelse som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId());
        }
    }

    private void håndterFødsel(Personhendelse payload) {
        var foedsel = payload.getFoedsel();
        if (foedsel != null) {
            LOG.info(
                    "FPABONNENT mottok fødsel: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} fødselsdato={} fødselsår={} fødested={} fødeKommune={} fødeland={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId(), foedsel.getFoedselsdato(), foedsel.getFoedselsaar(), foedsel.getFoedested(),
                    foedsel.getFoedekommune(), foedsel.getFoedeland());
        } else {
            LOG.info("FPABONNENT mottok fødsel: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId());
        }
        PdlFødsel pdlFødsel = oversetter.oversettFødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlFødsel);
    }

    private void håndterDødsfall(Personhendelse payload) {
        var doedsfall = payload.getDoedsfall();
        if (doedsfall != null) {
            LOG.info(
                    "FPABONNENT mottok dødsfall: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dødsdato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId(), doedsfall.getDoedsdato());
        } else {
            LOG.info("FPABONNENT mottok dødsfall: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId());
        }
        PdlDød pdlDød = oversetter.oversettDød(payload);
        prosesserHendelseVidereHvisRelevant(pdlDød);
    }

    private void håndterDødfødtBarn(Personhendelse payload) {
        var doedfoedtBarn = payload.getDoedfoedtBarn();
        if (doedfoedtBarn != null) {
            LOG.info(
                    "FPABONNENT mottok dødfødtBarn: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId(), doedfoedtBarn.getDato());
        } else {
            LOG.info("FPABONNENT mottok dødfødtBarn: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                    payload.getTidligereHendelseId());
        }
        PdlDødfødsel pdlDødfødsel = oversetter.oversettDødfødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlDødfødsel);
    }

    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId();
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }

    private void prosesserHendelseVidereHvisRelevant(PdlPersonhendelse personhendelse) {
        if (personhendelse.erRelevantForFpsak()) {
            LOG.info("Lagrer");
            InngåendeHendelse inngåendeHendelse = lagreInngåendeHendelse(personhendelse, HåndtertStatusType.MOTTATT);
            LOG.info("Finner neste");
            LocalDateTime håndteresEtterTidspunkt = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(inngåendeHendelse);
            LOG.info("Opppdaterer");
            hendelseRepository.oppdaterHåndteresEtterTidspunkt(inngåendeHendelse, håndteresEtterTidspunkt);
            LOG.info("Oppretter");
            opprettVurderSorteringTask(personhendelse, inngåendeHendelse.getId(), håndteresEtterTidspunkt);
            LOG.info("Opprettet OK");
        } else {
            LOG.info("Ikke-relevant hendelseId={} filtrert bort", personhendelse.getHendelseId());
        }
    }

    private InngåendeHendelse lagreInngåendeHendelse(PdlPersonhendelse personhendelse, HåndtertStatusType håndtertStatusType) {
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
                .hendelseType(personhendelse.getHendelseType())
                .hendelseId(personhendelse.getHendelseId())
                .tidligereHendelseId(personhendelse.getTidligereHendelseId())
                .payload(JsonMapper.toJson(personhendelse))
                .hendelseKilde(HendelseKilde.PDL)
                .håndtertStatus(håndtertStatusType)
                .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        return inngåendeHendelse;
    }

    private void opprettVurderSorteringTask(PdlPersonhendelse personhendelse, Long inngåendeHendelseId, LocalDateTime håndteresEtterTidspunkt) {
        HendelserDataWrapper vurderSorteringTask = new HendelserDataWrapper(new ProsessTaskData(VurderSorteringTask.TASKNAME));
        vurderSorteringTask.setInngåendeHendelseId(inngåendeHendelseId);
        vurderSorteringTask.setHendelseId(personhendelse.getHendelseId());
        vurderSorteringTask.setNesteKjøringEtter(håndteresEtterTidspunkt);
        vurderSorteringTask.setHendelseType(personhendelse.getHendelseType().getKode());
        prosessTaskRepository.lagre(vurderSorteringTask.getProsessTaskData());
    }
}
