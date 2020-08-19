package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.TpsHendelseHjelper.hentUtAktørIderFraString;

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
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlDødHendelsePayload;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØD_HENDELSE)
public class PdlDødHendelseTjeneste implements HendelseTjeneste<PdlDødHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødHendelseTjeneste.class);

    private PersonTjeneste personTjeneste;

    private HendelseRepository hendelseRepository;

    public PdlDødHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødHendelseTjeneste(PersonTjeneste personTjeneste, HendelseRepository hendelseRepository) {
        this.personTjeneste = personTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public PdlDødHendelsePayload payloadFraJsonString(String payload) {
        PdlDød pdlDød = JsonMapper.fromJson(payload, PdlDød.class);

        return new PdlDødHendelsePayload.Builder()
                .hendelseId(pdlDød.getHendelseId())
                .tidligereHendelseId(pdlDød.getTidligereHendelseId())
                .hendelseType(pdlDød.getHendelseType().getKode())
                .endringstype(pdlDød.getEndringstype().name())
                .hendelseOpprettetTid(pdlDød.getOpprettet())
                .aktørId(hentUtAktørIderFraString(pdlDød.getPersonidenter(), pdlDød.getHendelseId()))
                .dødsdato(pdlDød.getDødsdato())
                .build();
    }

    @Override
    public boolean vurderOmHendelseKanForkastes(PdlDødHendelsePayload payload) {
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

    private boolean sjekkOmHendelseHarSammeVerdiOgErSendt(PdlDødHendelsePayload payload, Optional<InngåendeHendelse> tidligereHendelse) {
        if (tidligereHendelse.isPresent() && tidligereHendelse.get().erSendtTilFpsak()) {
            boolean harSammeDato = payload.getDødsdato().isPresent() && payload.getDødsdato().equals(payloadFraJsonString(tidligereHendelse.get().getPayload()).getDødsdato());
            if (harSammeDato) {
                LOGGER.info("Hendelse {} vil bli forkastet da endringstypen er KORRIGERT, uten at dødsdatoen {} er endret siden hendelse {}, som er forrige hendelse som ble sendt til FPSAK",
                        payload.getHendelseId(), payload.getDødsdato(), tidligereHendelse.get().getHendelseId());
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
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlDødHendelsePayload payload) {
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (aktørIder.isPresent() && payload.getDødsdato().isPresent()) {
            if (harRegistrertDødsdato(aktørIder)) {
                return new KlarForSorteringResultat(true);
            }
        } else if (aktørIder.isPresent() && payload.getDødsdato().isEmpty() && PdlEndringstype.ANNULLERT.name().equals(payload.getEndringstype())) {
            return new KlarForSorteringResultat(true);
        }
        return new KlarForSorteringResultat(false);
    }

    @Override
    public void loggFeiletHendelse(PdlDødHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        Optional<LocalDate> dødsdato = payload.getDødsdato();
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (dødsdato.isEmpty()) {
            årsak = "Årsaken er at dødsdato mangler på hendelsen.";
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
            } else if (!harRegistrertDødsdato(aktørIder)) {
                årsak = "Årsaken er at det fortsatt ikke er registrert dødsdato i TPS.";
            }
        }
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
    }

    private Optional<InngåendeHendelse> getTidligereHendelse(String tidligereHendelseId) {
        return tidligereHendelseId != null ?
                hendelseRepository.finnHendelseFraIdHvisFinnes(tidligereHendelseId, HendelseKilde.PDL) :
                Optional.empty();
    }

    private boolean harRegistrertDødsdato(Optional<Set<String>> aktørIder) {
        for (String aktørId : aktørIder.get()) {
            if (personTjeneste.harRegistrertDødsdato(new AktørId(aktørId))) {
                return true;
            }
        }
        return false;
    }
}
