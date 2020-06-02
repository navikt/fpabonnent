package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraIdent;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.foedselsmelding.FoedselsmeldingOpprettet;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.FØDSELSMELDINGOPPRETTET)
public class FødselsmeldingOpprettetHendelseTjeneste implements HendelseTjeneste<FødselHendelsePayload> {

    public FødselsmeldingOpprettetHendelseTjeneste() {
        // CDI
    }

    @Override
    public FødselHendelsePayload payloadFraString(String payload) {
        FeedEntry entry = JsonMapper.fromJson(payload, FeedEntry.class);

        String json = JsonMapper.toJson(entry.getContent());
        FoedselsmeldingOpprettet foedselsmelding = JsonMapper.fromJson(json, FoedselsmeldingOpprettet.class);
        if (foedselsmelding == null) {
            throw AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent(entry.getType(), entry.getSequence()).toException();
        }
        return new FødselHendelsePayload.Builder()
                .hendelseId("" + entry.getSequence())
                .type(entry.getType())
                .aktørIdBarn(hentUtAktørIderFraIdent(foedselsmelding.getPersonIdenterBarn(), entry.getSequence()))
                .aktørIdMor(hentUtAktørIderFraIdent(foedselsmelding.getPersonIdenterMor(), entry.getSequence()))
                .aktørIdFar(hentUtAktørIderFraIdent(foedselsmelding.getPersonIdenterFar(), entry.getSequence()))
                .fødselsdato(foedselsmelding.getFoedselsdato())
                .build();
    }

    @Override
    public FødselHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new FødselHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .aktørIdBarn(dataWrapper.getAktørIdBarn().orElse(null))
                .aktørIdMor(dataWrapper.getAktørIdMor().orElse(null))
                .aktørIdFar(dataWrapper.getAktørIdFar().orElse(null))
                .fødselsdato(optionalStringTilLocalDate(dataWrapper.getFødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(FødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørIdBarn().ifPresent(dataWrapper::setAktørIdBarn);
        payload.getAktørIdMor().ifPresent(dataWrapper::setAktørIdMor);
        payload.getAktørIdFar().ifPresent(dataWrapper::setAktørIdFar);
        payload.getFødselsdato().ifPresent(dataWrapper::setFødselsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(FødselHendelsePayload payload) {
        return true;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(FødselHendelsePayload payload) {
        return new KlarForSorteringResultat(true);
    }
}
