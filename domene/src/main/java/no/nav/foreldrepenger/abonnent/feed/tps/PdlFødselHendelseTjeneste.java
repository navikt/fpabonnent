package no.nav.foreldrepenger.abonnent.feed.tps;

import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.hentUtAktørIderFraString;
import static no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper.optionalStringTilLocalDate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.felles.KlarForSorteringResultat;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.tps.AktørId;
import no.nav.foreldrepenger.abonnent.tps.PersonTjeneste;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.PDL_FØDSEL_HENDELSE)
public class PdlFødselHendelseTjeneste implements HendelseTjeneste<PdlFødselHendelsePayload> {

    private PersonTjeneste personTjeneste;

    @Inject
    public PdlFødselHendelseTjeneste(PersonTjeneste personTjeneste) {
        this.personTjeneste = personTjeneste;
    }

    @Override
    public PdlFødselHendelsePayload payloadFraString(String payload) {
        PdlFødsel pdlFødsel = JsonMapper.fromJson(payload, PdlFødsel.class);

        return new PdlFødselHendelsePayload.Builder()
                .hendelseId(pdlFødsel.getHendelseId())
                .type(pdlFødsel.getHendelseType().getKode())
                .endringstype(pdlFødsel.getEndringstype().name())
                .hendelseOpprettetTid(pdlFødsel.getOpprettet())
                .aktørIdBarn(hentUtAktørIderFraString(pdlFødsel.getPersonidenter(), pdlFødsel.getHendelseId()))
                .aktørIdForeldre(pdlFødsel.getAktørIdForeldre())
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
                .aktørIdForeldre(dataWrapper.getAktørIdForeldre().orElse(null))
                .fødselsdato(optionalStringTilLocalDate(dataWrapper.getFødselsdato()))
                .build();
    }

    @Override
    public void populerDatawrapper(PdlFødselHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        payload.getAktørIdBarn().ifPresent(dataWrapper::setAktørIdBarn);
        payload.getAktørIdForeldre().ifPresent(dataWrapper::setAktørIdForeldre);
        payload.getFødselsdato().ifPresent(dataWrapper::setFødselsdato);
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(PdlFødselHendelsePayload payload) {
        return true;
    }

    @Override
    public KlarForSorteringResultat vurderOmKlarForSortering(PdlFødselHendelsePayload payload) {
        if (payload.getAktørIdBarn().isPresent()) {
            Set<AktørId> foreldre = new HashSet<>();
            for (String aktørId : payload.getAktørIdBarn().get()) {
                foreldre.addAll(personTjeneste.registrerteForeldre(new AktørId(aktørId)));
            }
            if (!foreldre.isEmpty()) {
                FødselKlarForSorteringResultat resultat = new FødselKlarForSorteringResultat(true);
                resultat.setForeldre(foreldre.stream().map(AktørId::getId).collect(Collectors.toSet()));
                return resultat;

            }
        }
        return new FødselKlarForSorteringResultat(false);
    }

    @Override
    public void berikHendelseHvisNødvendig(InngåendeHendelse inngåendeHendelse, KlarForSorteringResultat klarForSorteringResultat) {
        PdlFødsel pdlFødsel = JsonMapper.fromJson(inngåendeHendelse.getPayload(), PdlFødsel.class);
        pdlFødsel.setAktørIdForeldre(((FødselKlarForSorteringResultat)klarForSorteringResultat).getForeldre());
        inngåendeHendelse.setPayload(JsonMapper.toJson(pdlFødsel));
    }

    private class FødselKlarForSorteringResultat extends KlarForSorteringResultat {

        private Set<String> foreldre;

        public FødselKlarForSorteringResultat(boolean resultat) {
            super(resultat);
        }

        public Set<String> getForeldre() {
            return foreldre;
        }

        public void setForeldre(Set<String> foreldre) {
            this.foreldre = foreldre;
        }
    }
}
