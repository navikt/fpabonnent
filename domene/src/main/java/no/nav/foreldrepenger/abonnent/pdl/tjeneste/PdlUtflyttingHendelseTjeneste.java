package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.pdl.tjeneste.HendelseTjenesteHjelper.hentUtAktørIderFraString;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlUtflytting;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlUtflyttingHendelsePayload;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_UTFLYTTING_HENDELSE)
public class PdlUtflyttingHendelseTjeneste implements HendelseTjeneste<PdlUtflyttingHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlUtflyttingHendelseTjeneste.class);

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;

    public PdlUtflyttingHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlUtflyttingHendelseTjeneste(HendelseTjenesteHjelper hendelseTjenesteHjelper) {
        this.hendelseTjenesteHjelper = hendelseTjenesteHjelper;
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
        if (payload.getAktørId().isPresent() && (payload.getUtflyttingsdato().isPresent()
                || payload.getUtflyttingsdato().isEmpty() && PdlEndringstype.ANNULLERT.name().equals(payload.getEndringstype()))) {
            return new KlarForSorteringResultat(true);
        }
        return new KlarForSorteringResultat(false);
    }

    @Override
    public void loggFeiletHendelse(PdlUtflyttingHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        if (payload.getUtflyttingsdato().isEmpty()) {
            årsak = "Årsaken er at utflyttingsdato mangler på hendelsen.";
        } else if (payload.getAktørId().isEmpty()) {
            årsak = "Årsaken er at aktørId mangler på hendelsen.";
        }
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
    }
}
