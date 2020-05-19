package no.nav.foreldrepenger.abonnent.pdl;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFamilierelasjon;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlPersonhendelse;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedfoedtbarn.DoedfoedtBarn;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.familierelasjon.Familierelasjon;
import no.nav.person.pdl.leesah.foedsel.Foedsel;
import no.nav.vedtak.log.mdc.MDCOperations;

@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseHåndterer.class);

    static final String FPABONNENT_GROVSORTERE_PDL = "fpabonnent.grovsortere.pdl";

    private PdlLeesahOversetter oversetter;
    private HendelseRepository hendelseRepository;
    private Unleash unleash;

    PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(HendelseRepository hendelseRepository,
                                      PdlLeesahOversetter pdlLeesahOversetter,
                                      Unleash unleash) {
        this.hendelseRepository = hendelseRepository;
        this.oversetter = pdlLeesahOversetter;
        this.unleash = unleash;
    }

    void handleMessage(String key, Personhendelse payload) { // key er spesialtegn + aktørId, som også finnes i payload
        setCallIdForHendelse(payload);

        Foedsel foedsel = payload.getFoedsel();
        if (foedsel != null) {
            LOG.info("FPABONNENT mottok fødsel: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} fødselsdato={} fødselsår={} fødested={} fødeKommune={} fødeland={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), foedsel.getFoedselsdato(), foedsel.getFoedselsaar(), foedsel.getFoedested(), foedsel.getFoedekommune(), foedsel.getFoedeland());
            PdlFødsel pdlFødsel = oversetter.oversettFødsel(payload);
            lagreInngåendeHendelseHvisRelevant(pdlFødsel);
        }

        Doedsfall doedsfall = payload.getDoedsfall();
        if (doedsfall != null) {
            LOG.info("FPABONNENT mottok dødsfall: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dødsdato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), doedsfall.getDoedsdato());
            PdlDød pdlDød = oversetter.oversettDød(payload);
            lagreInngåendeHendelseHvisRelevant(pdlDød);
        }

        DoedfoedtBarn doedfoedtBarn = payload.getDoedfoedtBarn();
        if (doedfoedtBarn != null) {
            LOG.info("FPABONNENT mottok dødfødtBarn: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} dato={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), doedfoedtBarn.getDato());
            PdlDødfødsel pdlDødfødsel = oversetter.oversettDødfødsel(payload);
            lagreInngåendeHendelseHvisRelevant(pdlDødfødsel);
        }

        Familierelasjon familierelasjon = payload.getFamilierelasjon();
        if (familierelasjon != null) {
            LOG.info("FPABONNENT mottok familierelasjon: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} relatertPersonsRolle={} minRolleForPerson={}",
                    payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId(), familierelasjon.getRelatertPersonsRolle(), familierelasjon.getMinRolleForPerson());
            PdlFamilierelasjon pdlFamilierelasjon = oversetter.oversettFamilierelasjon(payload);
            lagreInngåendeHendelseHvisRelevant(pdlFamilierelasjon);
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

    private void lagreInngåendeHendelseHvisRelevant(PdlPersonhendelse personhendelse) {
        if (personhendelse.erRelevantForFpsak()) {
            InngåendeHendelse.Builder inngåendeHendelse = InngåendeHendelse.builder()
                    .type(personhendelse.getHendelseType())
                    .hendelseId(personhendelse.getHendelseId())
                    .requestUuid(personhendelse.getHendelseId()) //TODO(JEJ): Fjerne felt når person-feed saneres?
                    .payload(JsonMapper.toJson(personhendelse))
                    .feedKode(FeedKode.PDL)
                    .håndteresEtterTidspunkt(LocalDateTime.now()); //TODO(JEJ): Legge inn TPS-forsinkelse, må ta høyde for helger

            if (unleash.isEnabled(FPABONNENT_GROVSORTERE_PDL, false)) {
                inngåendeHendelse.håndtertStatus(HåndtertStatusType.MOTTATT);
            } else {
                inngåendeHendelse.håndtertStatus(HåndtertStatusType.HÅNDTERT);
            }

            hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse.build());
        } else {
            LOG.info("Ikke-relevant hendelseId={} filtrert bort", personhendelse.getHendelseId());
        }
    }
}
