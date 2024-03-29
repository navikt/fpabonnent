package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtFødselsnumreFraString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseOpplysningType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlPersonhendelse;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.oppslag.ForeldreTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@HendelseTypeRef(HendelseOpplysningType.PDL_FØDSEL_HENDELSE)
public class PdlFødselHendelseTjeneste implements HendelseTjeneste<PdlFødselHendelsePayload> {

    private static final Logger LOG = LoggerFactory.getLogger(PdlFødselHendelseTjeneste.class);
    private static final Environment ENV = Environment.current();

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;
    private ForeldreTjeneste foreldreTjeneste;

    public PdlFødselHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlFødselHendelseTjeneste(HendelseTjenesteHjelper hendelseTjenesteHjelper, ForeldreTjeneste foreldreTjeneste) {
        this.hendelseTjenesteHjelper = hendelseTjenesteHjelper;
        this.foreldreTjeneste = foreldreTjeneste;
    }

    @Override
    public PdlFødselHendelsePayload payloadFraJsonString(String payload) {
        var pdlFødsel = DefaultJsonMapper.fromJson(payload, PdlFødsel.class);

        return new PdlFødselHendelsePayload.Builder().hendelseId(pdlFødsel.getHendelseId())
            .tidligereHendelseId(pdlFødsel.getTidligereHendelseId())
            .hendelseType(pdlFødsel.getHendelseType().getKode())
            .endringstype(pdlFødsel.getEndringstype().name())
            .hendelseOpprettetTid(pdlFødsel.getOpprettet())
            .fnrBarn(hentUtFødselsnumreFraString(pdlFødsel.getPersonidenter()))
            .aktørIdBarn(hentUtAktørIderFraString(pdlFødsel.getPersonidenter(), pdlFødsel.getHendelseId()))
            .aktørIdForeldre(pdlFødsel.getAktørIdForeldre())
            .fødselsdato(pdlFødsel.getFødselsdato())
            .build();
    }

    @Override
    public boolean vurderOmHendelseKanForkastes(PdlFødselHendelsePayload payload) {
        return hendelseTjenesteHjelper.vurderOmHendelseKanForkastes(payload, this::payloadFraJsonString);
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlFødselHendelsePayload payload) {
        if (!payload.getFnrBarn().isEmpty()) {
            var foreldre = getForeldre(payload.getFnrBarn());
            if (!foreldre.isEmpty()) {
                var resultat = new FødselKlarForSorteringResultat(true);
                resultat.setForeldre(foreldre.stream().map(AktørId::getId).collect(Collectors.toSet()));
                return resultat;
            } else if (payload.getFnrBarn().stream().allMatch(PersonIdent::erDnr)) {
                LOG.info("Fant ikke foreldre for hendelse {} med type {} og barn er DNR", payload.getHendelseId(), payload.getHendelseType());
                return new FødselKlarForSorteringResultat(false, false);
            } else if (!harRelevantFødselsdato(payload.getFnrBarn())) {
                LOG.info("For tidlig fødselsdato for hendelse {} med type {}", payload.getHendelseId(), payload.getHendelseType());
                return new FødselKlarForSorteringResultat(false, false);
            } else {
                LOG.info("Fant ikke foreldre for hendelse {} med type {}", payload.getHendelseId(), payload.getHendelseType());
                return new FødselKlarForSorteringResultat(false, true);
            }
        }
        LOG.warn("Hendelse {} med type {} har ikke barns fødselsnummer", payload.getHendelseId(), payload.getHendelseType());
        return new FødselKlarForSorteringResultat(false, false);
    }

    @Override
    public void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {
        var pdlFødsel = DefaultJsonMapper.fromJson(inngåendeHendelse.getPayload(), PdlFødsel.class);
        pdlFødsel.setAktørIdForeldre(((FødselKlarForSorteringResultat) klarForSorteringResultat).getForeldre());
        inngåendeHendelse.setPayload(DefaultJsonMapper.toJson(pdlFødsel));
    }

    @Override
    public void loggFeiletHendelse(PdlFødselHendelsePayload payload) {
        var basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        var årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        var info = false;
        if (payload.getFnrBarn().isEmpty()) {
            årsak = "Årsaken er at barnets fødselsnummer mangler på hendelsen.";
        } else if (getForeldre(payload.getFnrBarn()).isEmpty()) {
            årsak = "Årsaken er at barnet fortsatt ikke har registrerte foreldre i PDL.";
            info = true; // Innvandring blir varslet som fødsel OPPRETTET, men mangler ofte foreldreopplysninger (gjelder primært voksne)
        }
        var melding = basismelding + årsak;
        if (info) {
            LOG.info(melding, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
        } else {
            LOG.warn(melding, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
        }
    }

    private boolean harRelevantFødselsdato(Set<PersonIdent> barn) {
        for (var fnr : barn) {
            try {
                var fødselsdato = foreldreTjeneste.hentFødselsdato(fnr);
                if (fødselsdato.filter(f -> f.plus(PdlPersonhendelse.STØNADSPERIODE).isBefore(LocalDate.now())).isPresent()) {
                    return false;
                }
            } catch (TekniskException e) {
                if (ENV.isProd()) {
                    throw e;
                } else {
                    LOG.warn("Fikk feil ved kall til PDL, men lar mekanisme for å vurdere hendelsen på nytt håndtere feilen, siden miljøet er {}",
                        ENV.getCluster().clusterName(), e);
                }
            }
        }
        return true;
    }

    private Set<AktørId> getForeldre(Set<PersonIdent> fnrBarn) {
        Set<AktørId> foreldre = new HashSet<>();
        for (var fnr : fnrBarn) {
            try {
                foreldre.addAll(foreldreTjeneste.hentForeldre(fnr));
            } catch (TekniskException e) {
                if (ENV.isProd()) {
                    throw e;
                } else {
                    LOG.warn("Fikk feil ved kall til PDL, men lar mekanisme for å vurdere hendelsen på nytt håndtere feilen, siden miljøet er {}",
                        ENV.getCluster().clusterName(), e);
                }
            }
        }
        return foreldre;
    }

    private static class FødselKlarForSorteringResultat extends KlarForSorteringResultat {

        private Set<String> foreldre;

        public FødselKlarForSorteringResultat(boolean resultat) {
            super(resultat);
        }

        public FødselKlarForSorteringResultat(boolean resultat, boolean prøveIgjen) {
            super(resultat, prøveIgjen);
        }

        public Set<String> getForeldre() {
            return foreldre;
        }

        public void setForeldre(Set<String> foreldre) {
            this.foreldre = foreldre;
        }
    }
}
