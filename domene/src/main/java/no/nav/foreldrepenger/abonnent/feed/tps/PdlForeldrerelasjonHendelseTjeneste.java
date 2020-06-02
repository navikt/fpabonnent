package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraString;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.PdlForeldrerelasjonHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFamilierelasjon;

//TODO(JEJ): Slette koden som håndterer foreldrerelasjonshendelsen
@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_FAMILIERELASJON_HENDELSE)
public class PdlForeldrerelasjonHendelseTjeneste implements HendelseTjeneste<PdlForeldrerelasjonHendelsePayload> {

    public PdlForeldrerelasjonHendelseTjeneste() {
        // CDI
    }

    @Override
    public PdlForeldrerelasjonHendelsePayload payloadFraString(String payload) {
        PdlFamilierelasjon pdlFamilierelasjon = JsonMapper.fromJson(payload, PdlFamilierelasjon.class);

        return new PdlForeldrerelasjonHendelsePayload.Builder()
                .hendelseId(pdlFamilierelasjon.getHendelseId())
                .type(pdlFamilierelasjon.getHendelseType().getKode())
                .endringstype(pdlFamilierelasjon.getEndringstype().name())
                .hendelseOpprettetTid(pdlFamilierelasjon.getOpprettet())
                .aktørId(hentUtAktørIderFraString(pdlFamilierelasjon.getPersonidenter(), pdlFamilierelasjon.getHendelseId()))
                .build();
    }

    @Override
    public PdlForeldrerelasjonHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new PdlForeldrerelasjonHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .endringstype(dataWrapper.getEndringstype().orElse(null))
                .aktørId(dataWrapper.getAktørIdListe().orElse(null))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlForeldrerelasjonHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørId().ifPresent(dataWrapper::setAktørIdListe);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(PdlForeldrerelasjonHendelsePayload payload) {
        return true;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlForeldrerelasjonHendelsePayload payload) {
        return new KlarForSorteringResultat(true);
    }
}
