package no.nav.foreldrepenger.abonnent.pdl;

import static no.nav.foreldrepenger.abonnent.pdl.PdlLeesahOversetter.DØD;
import static no.nav.foreldrepenger.abonnent.pdl.PdlLeesahOversetter.DØDFØDSEL;
import static no.nav.foreldrepenger.abonnent.pdl.PdlLeesahOversetter.FAMILIERELASJON;
import static no.nav.foreldrepenger.abonnent.pdl.PdlLeesahOversetter.FØDSEL;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlPersonhendelse;
import no.nav.foreldrepenger.abonnent.task.VurderSorteringTask;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedfoedtbarn.DoedfoedtBarn;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.familierelasjon.Familierelasjon;
import no.nav.person.pdl.leesah.foedsel.Foedsel;
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
    private TpsForsinkelseTjeneste tpsForsinkelseTjeneste;
    private PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste;

    PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(HendelseRepository hendelseRepository,
                                      PdlLeesahOversetter pdlLeesahOversetter,
                                      ProsessTaskRepository prosessTaskRepository,
                                      TpsForsinkelseTjeneste tpsForsinkelseTjeneste,
                                      PdlFeatureToggleTjeneste pdlFeatureToggleTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.oversetter = pdlLeesahOversetter;
        this.prosessTaskRepository = prosessTaskRepository;
        this.tpsForsinkelseTjeneste = tpsForsinkelseTjeneste;
        this.pdlFeatureToggleTjeneste = pdlFeatureToggleTjeneste;
    }

    void handleMessage(String key, Personhendelse payload) { // key er spesialtegn + aktørId, som også finnes i payload
        setCallIdForHendelse(payload);

        Optional<InngåendeHendelse> inngåendeHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(payload.getHendelseId().toString(), FeedKode.PDL);
        if (inngåendeHendelse.isPresent()) {
            LOG.warn("FPABONNENT mottok duplikat hendelse som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
            return;
        }

        if (FØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterFødsel(payload);
        } else if (DØD.contentEquals(payload.getOpplysningstype())) {
            håndterDødsfall(payload);
        } else if (DØDFØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterDødfødtBarn(payload);
        } else if (FAMILIERELASJON.contentEquals(payload.getOpplysningstype())) {
            håndterFamilierelasjon(payload);
        } else {
            LOG.info("FPABONNENT mottok en ukjent hendelse som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
    }

    private void håndterFødsel(Personhendelse payload) {
        Foedsel foedsel = payload.getFoedsel();
        if (foedsel != null) {
            LOG.info("FPABONNENT mottok fødsel: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} fødselsdato={} fødselsår={} fødested={} fødeKommune={} fødeland={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), foedsel.getFoedselsdato(), foedsel.getFoedselsaar(), foedsel.getFoedested(), foedsel.getFoedekommune(), foedsel.getFoedeland());
        } else {
            LOG.info("FPABONNENT mottok fødsel: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
        PdlFødsel pdlFødsel = oversetter.oversettFødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlFødsel);
    }

    private void håndterDødsfall(Personhendelse payload) {
        Doedsfall doedsfall = payload.getDoedsfall();
        if (doedsfall != null) {
            LOG.info("FPABONNENT mottok dødsfall: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dødsdato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), doedsfall.getDoedsdato());
        } else {
            LOG.info("FPABONNENT mottok dødsfall: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
        PdlDød pdlDød = oversetter.oversettDød(payload);
        prosesserHendelseVidereHvisRelevant(pdlDød);
    }

    private void håndterDødfødtBarn(Personhendelse payload) {
        DoedfoedtBarn doedfoedtBarn = payload.getDoedfoedtBarn();
        if (doedfoedtBarn != null) {
            LOG.info("FPABONNENT mottok dødfødtBarn: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), doedfoedtBarn.getDato());
        } else {
            LOG.info("FPABONNENT mottok dødfødtBarn: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
        PdlDødfødsel pdlDødfødsel = oversetter.oversettDødfødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlDødfødsel);
    }

    private void håndterFamilierelasjon(Personhendelse payload) {
        Familierelasjon familierelasjon = payload.getFamilierelasjon();
        if (familierelasjon != null) {
            LOG.info("FPABONNENT mottok familierelasjon som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} relatertPersonsRolle={} minRolleForPerson={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), familierelasjon.getRelatertPersonsRolle(), familierelasjon.getMinRolleForPerson());
        } else {
            LOG.info("FPABONNENT mottok familierelasjon som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
    }

    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId(UUID.randomUUID().toString());
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }

    private void prosesserHendelseVidereHvisRelevant(PdlPersonhendelse personhendelse) {
        if (personhendelse.erRelevantForFpsak()) {
            if (pdlFeatureToggleTjeneste.skalLagrePdl()) {
                if (pdlFeatureToggleTjeneste.skalGrovsorterePdl()) {
                    InngåendeHendelse inngåendeHendelse = lagreInngåendeHendelse(personhendelse, HåndtertStatusType.MOTTATT);
                    LocalDateTime håndteresEtterTidspunkt = tpsForsinkelseTjeneste.finnNesteTidspunktForVurderSortering(personhendelse.getOpprettet(), inngåendeHendelse);
                    hendelseRepository.oppdaterHåndteresEtterTidspunkt(inngåendeHendelse, håndteresEtterTidspunkt);
                    opprettVurderSorteringTask(personhendelse, inngåendeHendelse.getId(), håndteresEtterTidspunkt);
                } else {
                    LOG.info("Grovsortering av hendelseId={} er deaktivert i dette clusteret", personhendelse.getHendelseId());
                    lagreInngåendeHendelse(personhendelse, HåndtertStatusType.HÅNDTERT);
                }
            } else {
                LOG.info("Lagring av hendelseId={} er deaktivert i dette clusteret", personhendelse.getHendelseId());
            }
        } else {
            LOG.info("Ikke-relevant hendelseId={} filtrert bort", personhendelse.getHendelseId());
        }
    }

    private InngåendeHendelse lagreInngåendeHendelse(PdlPersonhendelse personhendelse, HåndtertStatusType håndtertStatusType) {
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
                .type(personhendelse.getHendelseType())
                .hendelseId(personhendelse.getHendelseId())
                .tidligereHendelseId(personhendelse.getTidligereHendelseId())
                .requestUuid(personhendelse.getHendelseId()) //TODO(JEJ): Fjerne felt når person-feed saneres?
                .payload(JsonMapper.toJson(personhendelse))
                .feedKode(FeedKode.PDL)
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
