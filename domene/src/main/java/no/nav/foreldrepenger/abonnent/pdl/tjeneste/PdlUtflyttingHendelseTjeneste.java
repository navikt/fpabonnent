package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtAktørIderFraString;

import java.time.LocalDate;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlUtflytting;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlUtflyttingHendelsePayload;
import no.nav.foreldrepenger.abonnent.pdl.oppslag.UtflyttingsDatoTjeneste;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_UTFLYTTING_HENDELSE)
public class PdlUtflyttingHendelseTjeneste implements HendelseTjeneste<PdlUtflyttingHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlUtflyttingHendelseTjeneste.class);

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;
    private UtflyttingsDatoTjeneste utflyttingTjeneste;

    public PdlUtflyttingHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlUtflyttingHendelseTjeneste(HendelseTjenesteHjelper hendelseTjenesteHjelper,
                                         UtflyttingsDatoTjeneste utflyttingTjeneste) {
        this.hendelseTjenesteHjelper = hendelseTjenesteHjelper;
        this.utflyttingTjeneste = utflyttingTjeneste;
    }

    @Override
    public PdlUtflyttingHendelsePayload payloadFraJsonString(String payload) {
        var pdlUtflytting = JsonMapper.fromJson(payload, PdlUtflytting.class);

        return new PdlUtflyttingHendelsePayload.Builder()
                .hendelseId(pdlUtflytting.getHendelseId())
                .tidligereHendelseId(pdlUtflytting.getTidligereHendelseId())
                .hendelseType(pdlUtflytting.getHendelseType().getKode())
                .endringstype(pdlUtflytting.getEndringstype().name())
                .hendelseOpprettetTid(pdlUtflytting.getOpprettet())
                .aktørId(hentUtAktørIderFraString(pdlUtflytting.getPersonidenter(), pdlUtflytting.getHendelseId()))
                .utflyttingsdato(pdlUtflytting.getUtflyttingsdato())
                .build();
    }

    @Override
    public boolean vurderOmHendelseKanForkastes(PdlUtflyttingHendelsePayload payload) {
        return hendelseTjenesteHjelper.vurderOmHendelseKanForkastes(payload, this::payloadFraJsonString);
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlUtflyttingHendelsePayload payload) {
        var aktuellAktør = payload.getAktørId().orElse(Set.of()).stream().findFirst();
        if (aktuellAktør.isEmpty()) {
            LOGGER.warn("Hendelse {} med type {} har ikke aktørid", payload.getHendelseId(), payload.getHendelseType());
            return new UtflyttingKlarForSorteringResultat(false, false);
        }
        var resultat = new UtflyttingKlarForSorteringResultat(true);
        if (payload.getUtflyttingsdato().isEmpty() && PdlEndringstype.OPPRETTET.name().equals(payload.getEndringstype())) {
            resultat.setUtflyttingsdato(utflyttingTjeneste.finnUtflyttingsdato(aktuellAktør.get(), payload.getHendelseId()));
        } else {
            payload.getUtflyttingsdato().ifPresent(oppgittdato -> {
                var registerdato = utflyttingTjeneste.finnUtflyttingsdato(aktuellAktør.get(), payload.getHendelseId());
                if (oppgittdato.isAfter(registerdato)) {
                    LOGGER.warn("Utflyttingshendelse {} fant datoer: avvik oppgitt {} etter register {} - varsle daglig-overvåkning", payload.getHendelseId(), oppgittdato, registerdato);
                } else if (oppgittdato.isBefore(registerdato)) {
                    LOGGER.info("Utflyttingshendelse {} fant datoer: avvik oppgitt {} før register {}", payload.getHendelseId(), oppgittdato, registerdato);
                } else {
                    LOGGER.info("Utflyttingshendelse {} fant datoer: likhet oppgitt {} og register {}", payload.getHendelseId(), oppgittdato, registerdato);
                }
            });
        }
        return resultat;
    }

    @Override
    public void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {
        var identifisertDato = ((UtflyttingKlarForSorteringResultat)klarForSorteringResultat).getUtflyttingsdato();
        if (identifisertDato != null) {
            var utflytting = JsonMapper.fromJson(inngåendeHendelse.getPayload(), PdlUtflytting.class);
            utflytting.setUtflyttingsdato(identifisertDato);
            inngåendeHendelse.setPayload(JsonMapper.toJson(utflytting));
        }
    }

    @Override
    public void loggFeiletHendelse(PdlUtflyttingHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. {}";
        if (payload.getUtflyttingsdato().isEmpty()) {
            LOGGER.info(basismelding, "Årsaken er at utflyttingsdato mangler på hendelsen.", payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
        } else if (payload.getAktørId().isEmpty()) {
            LOGGER.warn(basismelding, "Årsaken er at aktørId mangler på hendelsen.", payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
        } else {
            LOGGER.warn(basismelding, "Årsaken er ukjent - bør undersøkes av utvikler.", payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
        }

    }

    private class UtflyttingKlarForSorteringResultat extends KlarForSorteringResultat {

        private LocalDate utflyttingsdato;

        public UtflyttingKlarForSorteringResultat(boolean resultat) {
            super(resultat);
        }

        public UtflyttingKlarForSorteringResultat(boolean resultat, boolean prøveIgjen) {
            super(resultat, prøveIgjen);
        }

        public LocalDate getUtflyttingsdato() {
            return utflyttingsdato;
        }

        public void setUtflyttingsdato(LocalDate utflyttingsdato) {
            this.utflyttingsdato = utflyttingsdato;
        }
    }
}
