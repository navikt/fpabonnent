package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.TpsHendelseHjelper.optionalStringTilLocalDate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlDødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØDFØDSEL_HENDELSE)
public class PdlDødfødselHendelseTjeneste implements HendelseTjeneste<PdlDødfødselHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødfødselHendelseTjeneste.class);

    private PersonTjeneste personTjeneste;

    private HendelseRepository hendelseRepository;

    public PdlDødfødselHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødfødselHendelseTjeneste(PersonTjeneste personTjeneste, HendelseRepository hendelseRepository) {
        this.personTjeneste = personTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public PdlDødfødselHendelsePayload payloadFraString(String payload) {
        PdlDødfødsel pdlDødfødsel = JsonMapper.fromJson(payload, PdlDødfødsel.class);

        return new PdlDødfødselHendelsePayload.Builder()
                .hendelseId(pdlDødfødsel.getHendelseId())
                .tidligereHendelseId(pdlDødfødsel.getTidligereHendelseId())
                .hendelseType(pdlDødfødsel.getHendelseType().getKode())
                .endringstype(pdlDødfødsel.getEndringstype().name())
                .hendelseOpprettetTid(pdlDødfødsel.getOpprettet())
                .aktørId(hentUtAktørIderFraString(pdlDødfødsel.getPersonidenter(), pdlDødfødsel.getHendelseId()))
                .dødfødselsdato(pdlDødfødsel.getDødfødselsdato())
                .build();
    }

    @Override
    public PdlDødfødselHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new PdlDødfødselHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .hendelseType(dataWrapper.getHendelseType().orElse(null))
                .endringstype(dataWrapper.getEndringstype().orElse(null))
                .aktørId(dataWrapper.getAktørIdListe().orElse(null))
                .dødfødselsdato(optionalStringTilLocalDate(dataWrapper.getDødfødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlDødfødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørId().ifPresent(dataWrapper::setAktørIdListe);
        payload.getDødfødselsdato().ifPresent(dataWrapper::setDødfødselsdato);
    }

    @Override
    public boolean vurderOmHendelseKanForkastes(PdlDødfødselHendelsePayload payload) {
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

    private boolean sjekkOmHendelseHarSammeVerdiOgErSendt(PdlDødfødselHendelsePayload payload, Optional<InngåendeHendelse> tidligereHendelse) {
        if (tidligereHendelse.isPresent() && tidligereHendelse.get().erSendtTilFpsak()) {
            boolean harSammeDato = payload.getDødfødselsdato().isPresent() && payload.getDødfødselsdato().equals(payloadFraString(tidligereHendelse.get().getPayload()).getDødfødselsdato());
            if (harSammeDato) {
                LOGGER.info("Hendelse {} vil bli forkastet da endringstypen er KORRIGERT, uten at dødfødselsdatoen {} er endret siden hendelse {}, som er forrige hendelse som ble sendt til FPSAK",
                        payload.getHendelseId(), payload.getDødfødselsdato(), tidligereHendelse.get().getHendelseId());
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
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlDødfødselHendelsePayload payload) {
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (aktørIder.isPresent() && payload.getDødfødselsdato().isPresent()) {
            if (harRegistrertDødfødsel(aktørIder, payload.getDødfødselsdato().get())) {
                return new KlarForSorteringResultat(true);
            }
        } else if (aktørIder.isPresent() && payload.getDødfødselsdato().isEmpty() && PdlEndringstype.ANNULLERT.name().equals(payload.getEndringstype())) {
            return new KlarForSorteringResultat(true);
        }
        return new KlarForSorteringResultat(false);
    }

    @Override
    public void loggFeiletHendelse(PdlDødfødselHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        Optional<LocalDate> dødfødselsdato = payload.getDødfødselsdato();
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (dødfødselsdato.isEmpty()) {
            årsak = "Årsaken er at dødfødselsdato mangler på hendelsen.";
        } else if (aktørIder.isEmpty()) {
            årsak = "Årsaken er at aktørId mangler på hendelsen.";
        } else {
            boolean aktørIkkeFunnetITPS = true;
            for (String aktørId : aktørIder.get()) {
                if (personTjeneste.erRegistrert(new AktørId(aktørId))) {
                    aktørIkkeFunnetITPS = false;
                }
            }
            if (aktørIkkeFunnetITPS) {
                årsak = "Årsaken er at aktørId fortsatt ikke finnes i TPS.";
            } else if (!harRegistrertDødfødsel(aktørIder, dødfødselsdato.get())) {
                årsak = "Årsaken er at dødfødselen fortsatt ikke er registrert i TPS.";
            }
        }
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
    }

    private Optional<InngåendeHendelse> getTidligereHendelse(String tidligereHendelseId) {
        return tidligereHendelseId != null ?
                hendelseRepository.finnHendelseFraIdHvisFinnes(tidligereHendelseId, HendelseKilde.PDL) :
                Optional.empty();
    }

    private boolean harRegistrertDødfødsel(Optional<Set<String>> aktørIder, LocalDate dødfødselsdato) {
        for (String aktørId : aktørIder.get()) {
            if (personTjeneste.harRegistrertDødfødsel(new AktørId(aktørId), dødfødselsdato)) {
                return true;
            }
        }
        return false;
    }
}
