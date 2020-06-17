package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.PdlDødHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDød;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØD_HENDELSE)
public class PdlDødHendelseTjeneste implements HendelseTjeneste<PdlDødHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødHendelseTjeneste.class);

    private PersonTjeneste personTjeneste;

    public PdlDødHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødHendelseTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    @Override
    public PdlDødHendelsePayload payloadFraString(String payload) {
        PdlDød pdlDød = JsonMapper.fromJson(payload, PdlDød.class);

        return new PdlDødHendelsePayload.Builder()
                .hendelseId(pdlDød.getHendelseId())
                .type(pdlDød.getHendelseType().getKode())
                .endringstype(pdlDød.getEndringstype().name())
                .hendelseOpprettetTid(pdlDød.getOpprettet())
                .aktørId(hentUtAktørIderFraString(pdlDød.getPersonidenter(), pdlDød.getHendelseId()))
                .dødsdato(pdlDød.getDødsdato())
                .build();
    }

    @Override
    public PdlDødHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new PdlDødHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .endringstype(dataWrapper.getEndringstype().orElse(null))
                .aktørId(dataWrapper.getAktørIdListe().orElse(null))
                .dødsdato(optionalStringTilLocalDate(dataWrapper.getDødsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlDødHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørId().ifPresent(dataWrapper::setAktørIdListe);
        payload.getDødsdato().ifPresent(dataWrapper::setDødsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(PdlDødHendelsePayload payload) {
        return true;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlDødHendelsePayload payload) {
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (aktørIder.isPresent() && payload.getDødsdato().isPresent()) {
            if (harRegistrertDødsdato(aktørIder)) {
                return new KlarForSorteringResultat(true);
            }
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
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getType(), payload.getHendelseOpprettetTid());
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
