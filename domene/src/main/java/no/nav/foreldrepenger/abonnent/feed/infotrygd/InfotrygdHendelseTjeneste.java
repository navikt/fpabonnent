package no.nav.foreldrepenger.abonnent.feed.infotrygd;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.InfotrygdHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.HendelseTypeRef;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.tjenester.InngåendeHendelseTjeneste;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Innhold;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.YTELSE_HENDELSE)
public class InfotrygdHendelseTjeneste implements HendelseTjeneste<InfotrygdHendelsePayload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfotrygdHendelseTjeneste.class);

    private InngåendeHendelseTjeneste inngåendeHendelseTjeneste;

    public InfotrygdHendelseTjeneste() {
        // CDI
    }

    @Inject
    public InfotrygdHendelseTjeneste(InngåendeHendelseTjeneste inngåendeHendelseTjeneste) {
        this.inngåendeHendelseTjeneste = inngåendeHendelseTjeneste;
    }

    @Override
    public InfotrygdHendelsePayload payloadFraString(String payload) {
        FeedElement entry = JsonMapper.fromJson(payload, FeedElement.class);

        String json = JsonMapper.toJson(entry.getInnhold());
        if (json == null || "null".equals(json)) {
            throw AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent(entry.getType(), entry.getSekvensId()).toException();
        }
        no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype meldingstype =
                no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype.fromType(entry.getType());
        Innhold innhold = (Innhold) JsonMapper.fromJson(json, meldingstype.getMeldingsDto());
        if (innhold == null) {
            throw AbonnentHendelserFeil.FACTORY.kanIkkeKonvertereFeedContent(entry.getType(), entry.getSekvensId()).toException();
        }
        return new InfotrygdHendelsePayload.Builder()
                .sekvensnummer(entry.getSekvensId())
                .koblingId(entry.getKoblingId())
                .type(entry.getType())
                .aktørId(innhold.getAktoerId())
                .fom(innhold.getFom())
                .identDato(innhold.getIdentDato())
                .typeYtelse(innhold.getTypeYtelse())
                .build();
    }

    @Override
    public InfotrygdHendelsePayload payloadFraWrapper(HendelserDataWrapper dataWrapper) {
        return new InfotrygdHendelsePayload.Builder()
                .aktørId(dataWrapper.getAktørId().orElse(null))
                .fom(dataWrapper.getFom().orElse(null))
                .identDato(dataWrapper.getIdentDato().orElse(null))
                .typeYtelse(dataWrapper.getTypeYtelse().orElse(null))
                .type(dataWrapper.getHendelseType().orElse(null))
                .sekvensnummer(dataWrapper.getHendelseSekvensnummer().orElse(null))
                .build();
    }

    @Override
    public void populerDatawrapper(InfotrygdHendelsePayload payload, HendelserDataWrapper dataWrapper) {
        dataWrapper.setAktørId(payload.getAktoerId());
        dataWrapper.setFom(payload.getFom());
        dataWrapper.setIdentDato(payload.getIdentDato());
        dataWrapper.setTypeYtelse(payload.getTypeYtelse());
    }

    @Override
    public boolean ikkeAtomiskHendelseSkalSendes(InfotrygdHendelsePayload payload) {
        List<InngåendeHendelse> alleHendelser = inngåendeHendelseTjeneste.finnAlleIkkeSorterteHendelserFraFeed(FeedKode.INFOTRYGD);
        for (InngåendeHendelse hendelse : alleHendelser) {
            if (hendelse.getKoblingId() != null && ikkeSammeSekvensnummer(payload, hendelse) &&
                    (hendelseHarKoblingIdLikPayloadsSekvensnummer(payload, hendelse) ||
                            hendelseOgPayloadHarSammeKoblingIdOgHendelsenHarHøyereSekvensnummer(payload, hendelse))) {
                LOGGER.info("Sender ikke Infotrygd hendelse med type {} og sekvensnummer {} da det finnes en senere hendelse med sekvensummer {} og koblingId {}",
                        payload.getType(), payload.getSekvensnummer(), hendelse.getSekvensnummer(), hendelse.getKoblingId());
                return false;
            }
        }
        return true;
    }

    private boolean ikkeSammeSekvensnummer(InfotrygdHendelsePayload itPayload, InngåendeHendelse hendelse) {
        return !hendelse.getSekvensnummer().equals(itPayload.getSekvensnummer());
    }

    private boolean hendelseHarKoblingIdLikPayloadsSekvensnummer(InfotrygdHendelsePayload itPayload, InngåendeHendelse hendelse) {
        return hendelse.getKoblingId().equals(itPayload.getSekvensnummer());
    }

    private boolean hendelseOgPayloadHarSammeKoblingIdOgHendelsenHarHøyereSekvensnummer(InfotrygdHendelsePayload itPayload, InngåendeHendelse hendelse) {
        return hendelse.getKoblingId() != 0L && hendelse.getKoblingId().equals(itPayload.getKoblingId()) && (hendelse.getSekvensnummer() > itPayload.getSekvensnummer());
    }
}
