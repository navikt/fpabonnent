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

import no.nav.foreldrepenger.abonnent.feed.domain.PdlDødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØDFØDSEL_HENDELSE)
public class PdlDødfødselHendelseTjeneste implements HendelseTjeneste<PdlDødfødselHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødfødselHendelseTjeneste.class);

    private PersonTjeneste personTjeneste;

    public PdlDødfødselHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødfødselHendelseTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    @Override
    public PdlDødfødselHendelsePayload payloadFraString(String payload) {
        PdlDødfødsel pdlDødfødsel = JsonMapper.fromJson(payload, PdlDødfødsel.class);

        return new PdlDødfødselHendelsePayload.Builder()
                .hendelseId(pdlDødfødsel.getHendelseId())
                .tidligereHendelseId(pdlDødfødsel.getTidligereHendelseId())
                .type(pdlDødfødsel.getHendelseType().getKode())
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
                .type(dataWrapper.getHendelseType().orElse(null))
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
    public boolean ikkeAtomiskHendelseSkalSendes(PdlDødfødselHendelsePayload payload) {
        return true;
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
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getType(), payload.getHendelseOpprettetTid());
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
