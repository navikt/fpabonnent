package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIder;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.abonnent.feed.domain.DødHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.doedsmelding.DoedsmeldingOpprettet;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.DØDSMELDINGOPPRETTET)
public class DødsmeldingOpprettetHendelseTjeneste implements HendelseTjeneste<DødHendelsePayload> {

    public DødsmeldingOpprettetHendelseTjeneste() {
        // CDI
    }

    @Override
    public DødHendelsePayload payloadFraString(String payload) {
        FeedEntry entry = JsonMapper.fromJson(payload, FeedEntry.class);

        String json = JsonMapper.toJson(entry.getContent());
        DoedsmeldingOpprettet dødsmelding = JsonMapper.fromJson(json, DoedsmeldingOpprettet.class);
        if (dødsmelding == null) {
            throw AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent(entry.getType(), entry.getSequence()).toException();
        }
        return new DødHendelsePayload.Builder()
                .sekvensnummer(entry.getSequence())
                .type(entry.getType())
                .aktørId(hentUtAktørIder(dødsmelding.getIdenter(), entry.getSequence()))
                .dødsdato(dødsmelding.getDoedsdato())
                .build();
    }

    @Override
    public DødHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new DødHendelsePayload.Builder()
                .sekvensnummer(dataWrapper.getHendelseSekvensnummer().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .aktørId(dataWrapper.getAktørIdListe().orElse(null))
                .dødsdato(optionalStringTilLocalDate(dataWrapper.getDødsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(DødHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørId().ifPresent(dataWrapper::setAktørIdListe);
        payload.getDødsdato().ifPresent(dataWrapper::setDødsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(DødHendelsePayload payload) {
        return true;
    }
}
