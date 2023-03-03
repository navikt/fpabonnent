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
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlDødHendelsePayload;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØD_HENDELSE)
public class PdlDødHendelseTjeneste implements HendelseTjeneste<PdlDødHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlDødHendelseTjeneste.class);

    private HendelseTjenesteHjelper hendelseTjenesteHjelper;

    public PdlDødHendelseTjeneste() {
        // CDI
    }

    @Inject
    public PdlDødHendelseTjeneste(HendelseTjenesteHjelper hendelseTjenesteHjelper) {
        this.hendelseTjenesteHjelper = hendelseTjenesteHjelper;
    }

    @Override
    public PdlDødHendelsePayload payloadFraJsonString(String payload) {
        PdlDød pdlDød = JsonMapper.fromJson(payload, PdlDød.class);

        return new PdlDødHendelsePayload.Builder().hendelseId(pdlDød.getHendelseId())
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
        return hendelseTjenesteHjelper.vurderOmHendelseKanForkastes(payload, this::payloadFraJsonString);
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlDødHendelsePayload payload) {
        if (payload.getAktørId().isPresent() && (payload.getDødsdato().isPresent()
            || payload.getDødsdato().isEmpty() && PdlEndringstype.ANNULLERT.name().equals(payload.getEndringstype()))) {
            return new KlarForSorteringResultat(true);
        }
        return new KlarForSorteringResultat(false);
    }

    @Override
    public void loggFeiletHendelse(PdlDødHendelsePayload payload) {
        String basismelding = "Hendelse {} med type {} som ble opprettet {} kan fremdeles ikke sorteres og blir derfor ikke behandlet videre. ";
        String årsak = "Årsaken er ukjent - bør undersøkes av utvikler.";
        if (payload.getDødsdato().isEmpty()) {
            årsak = "Årsaken er at dødsdato mangler på hendelsen.";
        } else if (payload.getAktørId().isEmpty()) {
            årsak = "Årsaken er at aktørId mangler på hendelsen.";
        }
        LOGGER.warn(basismelding + årsak, payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseOpprettetTid());
    }
}
