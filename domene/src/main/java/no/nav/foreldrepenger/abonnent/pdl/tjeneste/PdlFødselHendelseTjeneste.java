package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.TpsHendelseHjelper.optionalStringTilLocalDate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.FeedKode;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_FØDSEL_HENDELSE)
public class PdlFødselHendelseTjeneste implements HendelseTjeneste<PdlFødselHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlFødselHendelseTjeneste.class);

    private PersonTjeneste personTjeneste;

    private HendelseRepository hendelseRepository;

    public PdlFødselHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlFødselHendelseTjeneste(PersonTjeneste personTjeneste, HendelseRepository hendelseRepository) {
        this.personTjeneste = personTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public PdlFødselHendelsePayload payloadFraString(String payload) {
        PdlFødsel pdlFødsel = JsonMapper.fromJson(payload, PdlFødsel.class);

        return new PdlFødselHendelsePayload.Builder()
                .hendelseId(pdlFødsel.getHendelseId())
                .tidligereHendelseId(pdlFødsel.getTidligereHendelseId())
                .type(pdlFødsel.getHendelseType().getKode())
                .endringstype(pdlFødsel.getEndringstype().name())
                .hendelseOpprettetTid(pdlFødsel.getOpprettet())
                .aktørIdBarn(hentUtAktørIderFraString(pdlFødsel.getPersonidenter(), pdlFødsel.getHendelseId()))
                .aktørIdForeldre(pdlFødsel.getAktørIdForeldre())
                .fødselsdato(pdlFødsel.getFødselsdato())
                .build();
    }

    @Override
    public PdlFødselHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new PdlFødselHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .endringstype(dataWrapper.getEndringstype().orElse(null))
                .aktørIdBarn(dataWrapper.getAktørIdBarn().orElse(null))
                .aktørIdForeldre(dataWrapper.getAktørIdForeldre().orElse(null))
                .fødselsdato(optionalStringTilLocalDate(dataWrapper.getFødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlFødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørIdBarn().ifPresent(dataWrapper::setAktørIdBarn);
        payload.getAktørIdForeldre().ifPresent(dataWrapper::setAktørIdForeldre);
        payload.getFødselsdato().ifPresent(dataWrapper::setFødselsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(PdlFødselHendelsePayload payload) {
        return true;
    }

    @Override
    public boolean vurderOmHendelseKanForkastes(PdlFødselHendelsePayload payload) {
        if (payload.getFødselsdato().isPresent() && payload.getFødselsdato().get().isBefore(LocalDate.now().minusYears(2))) {
            LOGGER.info("Hendelse {} har fødselsdato {} som var for mer enn to år siden og blir derfor forkastet",
                    payload.getHendelseId(), payload.getFødselsdato().get());
            return true;
        }
        Optional<InngåendeHendelse> tidligereHendelse = getTidligereHendelse(payload.getTidligereHendelseId());
        if (of(PdlEndringstype.ANNULLERT.name(), PdlEndringstype.KORRIGERT.name()).contains(payload.getEndringstype())
                && tidligereHendelse.isEmpty()) {
            LOGGER.info("Hendelse {} vil bli forkastet da endringstypen er {}, uten at vi har mottatt tidligere hendelse {}",
                    payload.getHendelseId(), payload.getEndringstype(), payload.getTidligereHendelseId());
            return true;
        } else if (PdlEndringstype.KORRIGERT.name().equals(payload.getEndringstype()) && tidligereHendelse.isPresent()) {
            return sjekkOmHendelseHarSammeVerdiOgErSendt(payload, tidligereHendelse);
        }
        return false;
    }

    private boolean sjekkOmHendelseHarSammeVerdiOgErSendt(PdlFødselHendelsePayload payload, Optional<InngåendeHendelse> tidligereHendelse) {
        if (tidligereHendelse.isPresent() && tidligereHendelse.get().erSendtTilFpsak()) {
            boolean harSammeDato = payload.getFødselsdato().isPresent() && payload.getFødselsdato().equals(payloadFraString(tidligereHendelse.get().getPayload()).getFødselsdato());
            if (harSammeDato) {
                LOGGER.info("Hendelse {} vil bli forkastet da endringstypen er KORRIGERT, uten at fødselsdatoen {} er endret siden hendelse {}, som er forrige hendelse som ble sendt til FPSAK",
                        payload.getHendelseId(), payload.getFødselsdato(), tidligereHendelse.get().getHendelseId());
            }
            return harSammeDato;
        } else if (tidligereHendelse.isPresent() && tidligereHendelse.get().erFerdigbehandletMenIkkeSendtTilFpsak() && tidligereHendelse.get().getTidligereHendelseId() != null) {
            // Gjøre sammenlikningen mot neste tidligere hendelse i stedet, i tilfelle den er sendt til Fpsak
            Optional<InngåendeHendelse> nesteTidligereHendelse = getTidligereHendelse(tidligereHendelse.get().getTidligereHendelseId());
            return nesteTidligereHendelse.isPresent() && sjekkOmHendelseHarSammeVerdiOgErSendt(payload, nesteTidligereHendelse);
        }
        return false;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlFødselHendelsePayload payload) {
        if (payload.getAktørIdBarn().isPresent()) {
            Set<AktørId> foreldre = getForeldre(payload);
            if (!foreldre.isEmpty()) {
                FødselKlarForSorteringResultat resultat = new FødselKlarForSorteringResultat(true);
                resultat.setForeldre(foreldre.stream().map(AktørId::getId).collect(Collectors.toSet()));
                return resultat;
            }
        } else {
            LOGGER.warn("Hendelse {} med type {} har ikke barns aktørId", payload.getHendelseId(), payload.getType());
        }
        return new FødselKlarForSorteringResultat(false);
    }

    @Override
    public void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {
        PdlFødsel pdlFødsel = JsonMapper.fromJson(inngåendeHendelse.getPayload(), PdlFødsel.class);
        pdlFødsel.setAktørIdForeldre(((FødselKlarForSorteringResultat)klarForSorteringResultat).getForeldre());
        inngåendeHendelse.setPayload(JsonMapper.toJson(pdlFødsel));
    }

    @Override
    public void loggFeiletHendelse(PdlFødselHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        boolean info = false;
        Optional<Set<String>> aktørIdBarn = payload.getAktørIdBarn();
        if (aktørIdBarn.isEmpty()) {
            årsak = "Årsaken er at barnets aktørId mangler på hendelsen.";
        } else {
            boolean barnIkkeFunnetITPS = true;
            for (String aktørId : aktørIdBarn.get()) {
                if (personTjeneste.erRegistrert(new AktørId(aktørId))) {
                    barnIkkeFunnetITPS = false;
                }
            }
            if (barnIkkeFunnetITPS) {
                årsak = "Årsaken er at barnet fortsatt ikke finnes i TPS.";
            } else if (getForeldre(payload).isEmpty()) {
                årsak = "Årsaken er at barnet fortsatt ikke har registrerte foreldre i TPS.";
                info = true; // Innvandring blir varslet som fødsel OPPRETTET, men mangler ofte foreldreopplysninger (gjelder primært voksne)
            }
        }
        if (info) {
            LOGGER.info(basismelding + årsak, payload.getHendelseId(), payload.getType(), payload.getHendelseOpprettetTid());
        } else {
            LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getType(), payload.getHendelseOpprettetTid());
        }
    }

    private Optional<InngåendeHendelse> getTidligereHendelse(String tidligereHendelseId) {
        return tidligereHendelseId != null ?
                hendelseRepository.finnHendelseFraIdHvisFinnes(tidligereHendelseId, FeedKode.PDL) :
                Optional.empty();
    }

    private Set<AktørId> getForeldre(PdlFødselHendelsePayload payload) {
        Set<AktørId> foreldre = new HashSet<>();
        for (String aktørId : payload.getAktørIdBarn().get()) {
            foreldre.addAll(personTjeneste.registrerteForeldre(new AktørId(aktørId)));
        }
        return foreldre;
    }

    private class FødselKlarForSorteringResultat extends KlarForSorteringResultat {

        private Set<String> foreldre;

        public FødselKlarForSorteringResultat(boolean resultat) {
            super(resultat);
        }

        public Set<String> getForeldre() {
            return foreldre;
        }

        public void setForeldre(Set<String> foreldre) {
            this.foreldre = foreldre;
        }
    }
}
