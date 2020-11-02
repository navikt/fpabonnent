package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtAktørIderFraString;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlDødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØDFØDSEL_HENDELSE)
public class PdlDødfødselHendelseTjeneste implements HendelseTjeneste<PdlDødfødselHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødfødselHendelseTjeneste.class);
    private static final Environment ENV = Environment.current();

    private PersonTjeneste personTjeneste;

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;

    public PdlDødfødselHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødfødselHendelseTjeneste(PersonTjeneste personTjeneste, HendelseTjenesteHjelper hendelseTjenesteHjelper) {
        this.personTjeneste = personTjeneste;
        this.hendelseTjenesteHjelper = hendelseTjenesteHjelper;
    }

    @Override
    public PdlDødfødselHendelsePayload payloadFraJsonString(String payload) {
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
    public boolean vurderOmHendelseKanForkastes(PdlDødfødselHendelsePayload payload) {
        return hendelseTjenesteHjelper.vurderOmHendelseKanForkastes(payload, this::payloadFraJsonString);
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlDødfødselHendelsePayload payload) {
        Optional<Set<String>> aktørIder = payload.getAktørId();
        if (aktørIder.isPresent() && payload.getDødfødselsdato().isPresent()) {
            if (aktørErRegistrertITps(aktørIder.get())) {
                // Vi kan ikke lengre sjekke om dødfødselen finnes i TPS da det ikke blir oppdatert med dette lengre
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
        } else if (!aktørErRegistrertITps(aktørIder.get())) {
            årsak = "Årsaken er at aktørId fortsatt ikke finnes i TPS.";
        }
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
    }

    private boolean aktørErRegistrertITps(Set<String> aktørIder) {
        for (String aktørId : aktørIder) {
            try {
                if (personTjeneste.erRegistrert(new AktørId(aktørId))) {
                    return true;
                }
            } catch (TekniskException e) {
                if (ENV.isProd()) {
                    throw e;
                } else {
                    LOGGER.warn("Fikk feil ved kall til TPS, men lar mekanisme for å vurdere hendelsen på nytt håndtere feilen, siden miljøet er {}", ENV.getCluster().clusterName(), e);
                }
            }
        }
        return false;
    }
}
