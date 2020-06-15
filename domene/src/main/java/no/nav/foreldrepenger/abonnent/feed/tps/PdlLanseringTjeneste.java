package no.nav.foreldrepenger.abonnent.feed.tps;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.DødHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.DødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.PdlDødHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.PdlDødfødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;

/**
 * Midlertidig tjeneste for å unngå duplikater i de dagene vi konsumerer både TPS- og PDL- hendelser.
 */
@ApplicationScoped
public class PdlLanseringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLanseringTjeneste.class);

    private FødselsmeldingOpprettetHendelseTjeneste fødselsmeldingOpprettetHendelseTjeneste;
    private DødsmeldingOpprettetHendelseTjeneste dødsmeldingOpprettetHendelseTjeneste;
    private DødfødselOpprettetHendelseTjeneste dødfødselOpprettetHendelseTjeneste;
    private PdlFødselHendelseTjeneste pdlFødselHendelseTjeneste;
    private PdlDødHendelseTjeneste pdlDødHendelseTjeneste;
    private PdlDødfødselHendelseTjeneste pdlDødfødselHendelseTjeneste;
    private HendelseRepository hendelseRepository;

    public PdlLanseringTjeneste() {
        // CDI
    }

    @Inject
    public PdlLanseringTjeneste(@Any FødselsmeldingOpprettetHendelseTjeneste fødselsmeldingOpprettetHendelseTjeneste,
                                @Any DødsmeldingOpprettetHendelseTjeneste dødsmeldingOpprettetHendelseTjeneste,
                                @Any DødfødselOpprettetHendelseTjeneste dødfødselOpprettetHendelseTjeneste,
                                @Any PdlFødselHendelseTjeneste pdlFødselHendelseTjeneste,
                                @Any PdlDødHendelseTjeneste pdlDødHendelseTjeneste,
                                @Any PdlDødfødselHendelseTjeneste pdlDødfødselHendelseTjeneste,
                                HendelseRepository hendelseRepository) {
        this.fødselsmeldingOpprettetHendelseTjeneste = fødselsmeldingOpprettetHendelseTjeneste;
        this.dødsmeldingOpprettetHendelseTjeneste = dødsmeldingOpprettetHendelseTjeneste;
        this.dødfødselOpprettetHendelseTjeneste = dødfødselOpprettetHendelseTjeneste;
        this.pdlFødselHendelseTjeneste = pdlFødselHendelseTjeneste;
        this.pdlDødHendelseTjeneste = pdlDødHendelseTjeneste;
        this.pdlDødfødselHendelseTjeneste = pdlDødfødselHendelseTjeneste;
        this.hendelseRepository = hendelseRepository;
    }

    public Optional<String> sjekkOmTpsHendelseErMottattFraPdlAllerede(FeedEntry feedEntry) {
        HendelseType hendelseType = HendelseType.fraKodeDefaultUdefinert(feedEntry.getType());

        if (HendelseType.FØDSELSMELDINGOPPRETTET.equals(hendelseType)) {
            FødselHendelsePayload tpsPayload = fødselsmeldingOpprettetHendelseTjeneste.payloadFraString(JsonMapper.toJson(feedEntry));
            List<InngåendeHendelse> pdlHendelser = hendelseRepository.finnAlleHendelserFraSisteUkeAvType(HendelseType.PDL_FØDSEL_OPPRETTET, FeedKode.PDL);
            for (InngåendeHendelse pdlHendelse : pdlHendelser) {
                PdlFødselHendelsePayload pdlPayload = pdlFødselHendelseTjeneste.payloadFraString(pdlHendelse.getPayload());
                if (tpsPayload.getAktørIdBarn().isPresent() && pdlPayload.getAktørIdBarn().isPresent()) {
                    Optional<Boolean> match = tpsPayload.getAktørIdBarn().get().stream()
                            .map(aktørId -> pdlPayload.getAktørIdBarn().get().contains(aktørId)).findFirst();
                    if (match.isPresent() && match.get()) {
                        return Optional.of(pdlHendelse.getHendelseId());
                    }
                }
            }

        } else if (HendelseType.DØDSMELDINGOPPRETTET.equals(hendelseType)) {
            DødHendelsePayload tpsPayload = dødsmeldingOpprettetHendelseTjeneste.payloadFraString(JsonMapper.toJson(feedEntry));
            List<InngåendeHendelse> pdlHendelser = hendelseRepository.finnAlleHendelserFraSisteUkeAvType(HendelseType.PDL_DØD_OPPRETTET, FeedKode.PDL);
            for (InngåendeHendelse pdlHendelse : pdlHendelser) {
                PdlDødHendelsePayload pdlPayload = pdlDødHendelseTjeneste.payloadFraString(pdlHendelse.getPayload());
                if (tpsPayload.getAktørId().isPresent() && pdlPayload.getAktørId().isPresent()) {
                    Optional<Boolean> match = tpsPayload.getAktørId().get().stream()
                            .map(aktørId -> pdlPayload.getAktørId().get().contains(aktørId)).findFirst();
                    if (match.isPresent() && match.get()) {
                        return Optional.of(pdlHendelse.getHendelseId());
                    }
                }
            }

        } else if (HendelseType.DØDFØDSELOPPRETTET.equals(hendelseType)) {
            DødfødselHendelsePayload tpsPayload = dødfødselOpprettetHendelseTjeneste.payloadFraString(JsonMapper.toJson(feedEntry));
            List<InngåendeHendelse> pdlHendelser = hendelseRepository.finnAlleHendelserFraSisteUkeAvType(HendelseType.PDL_DØDFØDSEL_OPPRETTET, FeedKode.PDL);
            for (InngåendeHendelse pdlHendelse : pdlHendelser) {
                PdlDødfødselHendelsePayload pdlPayload = pdlDødfødselHendelseTjeneste.payloadFraString(pdlHendelse.getPayload());
                if (tpsPayload.getAktørId().isPresent() && pdlPayload.getAktørId().isPresent()) {
                    Optional<Boolean> match = tpsPayload.getAktørId().get().stream()
                            .map(aktørId -> pdlPayload.getAktørId().get().contains(aktørId)).findFirst();
                    if (match.isPresent() && match.get()) {
                        return Optional.of(pdlHendelse.getHendelseId());
                    }
                }
            }

        } else {
            LOG.warn("Ukjent TPS hendelsetype {} mottatt", feedEntry.getType());
        }
        return Optional.empty();
    }
}
