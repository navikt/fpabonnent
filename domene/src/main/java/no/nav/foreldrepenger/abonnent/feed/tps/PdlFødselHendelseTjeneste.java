package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_FØDSEL_HENDELSE)
public class PdlFødselHendelseTjeneste implements HendelseTjeneste<PdlFødselHendelsePayload> {

    public PdlFødselHendelseTjeneste() {
        // CDI
    }

    @Override
    public PdlFødselHendelsePayload payloadFraString(String payload) {
        PdlFødsel pdlFødsel = JsonMapper.fromJson(payload, PdlFødsel.class);

        return new PdlFødselHendelsePayload.Builder()
                .hendelseId(pdlFødsel.getHendelseId())
                .type(pdlFødsel.getHendelseType().getKode())
                .endringstype(pdlFødsel.getEndringstype().name())
                .aktørIdBarn(hentUtAktørIderFraString(pdlFødsel.getPersonidenter(), pdlFødsel.getHendelseId()))
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
                .fødselsdato(optionalStringTilLocalDate(dataWrapper.getFødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlFødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørIdBarn().ifPresent(dataWrapper::setAktørIdBarn);
        payload.getFødselsdato().ifPresent(dataWrapper::setFødselsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(PdlFødselHendelsePayload payload) {
        return true;
    }
}
