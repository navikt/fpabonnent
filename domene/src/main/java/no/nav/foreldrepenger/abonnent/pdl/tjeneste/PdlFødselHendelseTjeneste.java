package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtFødselsnumreFraString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.AktørId;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.oppslag.ForeldreTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_FØDSEL_HENDELSE)
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
        PdlFødsel pdlFødsel = JsonMapper.fromJson(payload, PdlFødsel.class);

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
        var fødselsdato = payload.getFødselsdato();
        if (fødselsdato.isPresent() && fødselsdato.get().isBefore(LocalDate.now().minusYears(2))) {
            LOG.info("Hendelse {} har fødselsdato {} som var for mer enn to år siden og blir derfor forkastet", payload.getHendelseId(),
                fødselsdato.get());
            return true;
        }
        return hendelseTjenesteHjelper.vurderOmHendelseKanForkastes(payload, this::payloadFraJsonString);
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlFødselHendelsePayload payload) {
        if (!payload.getFnrBarn().isEmpty()) {
            Set<AktørId> foreldre = getForeldre(payload.getFnrBarn());
            if (!foreldre.isEmpty()) {
                FødselKlarForSorteringResultat resultat = new FødselKlarForSorteringResultat(true);
                resultat.setForeldre(foreldre.stream().map(AktørId::getId).collect(Collectors.toSet()));
                return resultat;
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
        PdlFødsel pdlFødsel = JsonMapper.fromJson(inngåendeHendelse.getPayload(), PdlFødsel.class);
        pdlFødsel.setAktørIdForeldre(((FødselKlarForSorteringResultat) klarForSorteringResultat).getForeldre());
        inngåendeHendelse.setPayload(JsonMapper.toJson(pdlFødsel));
    }

    @Override
    public void loggFeiletHendelse(PdlFødselHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        boolean info = false;
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

    private Set<AktørId> getForeldre(Set<PersonIdent> fnrBarn) {
        Set<AktørId> foreldre = new HashSet<>();
        for (PersonIdent fnr : fnrBarn) {
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

    private class FødselKlarForSorteringResultat extends KlarForSorteringResultat {

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
