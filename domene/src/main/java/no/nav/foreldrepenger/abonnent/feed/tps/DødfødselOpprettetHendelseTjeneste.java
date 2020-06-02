package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraIdent;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.DødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.doedfoedsel.DoedfoedselOpprettet;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.DØDFØDSELOPPRETTET)
public class DødfødselOpprettetHendelseTjeneste implements HendelseTjeneste<DødfødselHendelsePayload> {

    public DødfødselOpprettetHendelseTjeneste() {
        // CDI
    }

    @Override
    public DødfødselHendelsePayload payloadFraString(String payload) {
        FeedEntry entry = JsonMapper.fromJson(payload, FeedEntry.class);

        String json = JsonMapper.toJson(entry.getContent());
        DoedfoedselOpprettet dødfødsel = JsonMapper.fromJson(json, DoedfoedselOpprettet.class);
        if (dødfødsel == null) {
            throw AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent(entry.getType(), entry.getSequence()).toException();
        }
        return new DødfødselHendelsePayload.Builder()
                .hendelseId("" + entry.getSequence())
                .type(entry.getType())
                .aktørId(hentUtAktørIderFraIdent(dødfødsel.getIdenter(), entry.getSequence()))
                .dødfødselsdato(dødfødsel.getDoedfoedselsdato())
                .build();
    }

    @Override
    public DødfødselHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new DødfødselHendelsePayload.Builder()
                .hendelseId(dataWrapper.getHendelseId().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .aktørId(dataWrapper.getAktørIdListe().orElse(null))
                .dødfødselsdato(optionalStringTilLocalDate(dataWrapper.getDødfødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(DødfødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørId().ifPresent(dataWrapper::setAktørIdListe);
        payload.getDødfødselsdato().ifPresent(dataWrapper::setDødfødselsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(DødfødselHendelsePayload payload) {
        return true;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(DødfødselHendelsePayload payload) {
        return new KlarForSorteringResultat(true);
    }
}
