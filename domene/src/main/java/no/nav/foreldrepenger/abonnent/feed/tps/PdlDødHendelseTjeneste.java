package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.PdlDødHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDød;


@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_DØD_HENDELSE)
public class PdlDødHendelseTjeneste implements HendelseTjeneste<PdlDødHendelsePayload> {

    public PdlDødHendelseTjeneste() {
        // CDI
    }

    @Override
    public PdlDødHendelsePayload payloadFraString(String payload) {
        PdlDød pdlDød = JsonMapper.fromJson(payload, PdlDød.class);

        return new PdlDødHendelsePayload.Builder()
                .hendelseId(pdlDød.getHendelseId())
                .type(pdlDød.getHendelseType().getKode())
                .endringstype(pdlDød.getEndringstype().name())
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
}
