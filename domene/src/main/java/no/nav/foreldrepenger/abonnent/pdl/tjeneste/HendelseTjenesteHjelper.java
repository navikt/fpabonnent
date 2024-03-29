package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static no.nav.foreldrepenger.abonnent.felles.domene.HendelseType.PDL_UTFLYTTING_ANNULLERT;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.pdl.domene.PersonIdent;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;

@ApplicationScoped
public class HendelseTjenesteHjelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseTjenesteHjelper.class);

    private HendelseRepository hendelseRepository;

    public HendelseTjenesteHjelper() {
        // CDI
    }

    @Inject
    public HendelseTjenesteHjelper(HendelseRepository hendelseRepository) {
        this.hendelseRepository = hendelseRepository;
    }

    /**
     * Sjekker om angitt payload kan forkastes. Vil forkaste hvis:
     * - Endringstypen er ANNULLERT/KORRIGERT og tidligere hendelse ikke er mottatt av oss.
     * - Endringstypen er KORRIGERT og datoen for hendelsen er samme som i siste sendte tidligere hendelse.
     * - Annullert utflytting som er mer enn 1 uke etter tidligere hendelse (unngå revurderinger i utide)
     *
     * @param payload              HendelsePayload som skal vurderes
     * @param payloadFraJsonString Referanse til funksjonen i HendelseTjeneste som oversetter fra JSON til HendelsePayload
     * @return true hvis hendelsen kan forkastes
     */
    public boolean vurderOmHendelseKanForkastes(HendelsePayload payload, Function<String, HendelsePayload> payloadFraJsonString) {
        var tidligereHendelse = getTidligereHendelse(payload.getTidligereHendelseId());
        if (PdlEndringstype.KORRIGERT.name().equals(payload.getEndringstype()) && tidligereHendelse.isPresent()) {
            return sjekkOmHendelseHarSammeVerdiOgErSendt(payload, tidligereHendelse, payloadFraJsonString);
        } else if (PDL_UTFLYTTING_ANNULLERT.getKode().equals(payload.getHendelseType()) && tidligereHendelse.isPresent()) {
            return tidligereHendelse.filter(h -> h.getOpprettetTidspunkt().plusWeeks(1).isBefore(payload.getHendelseOpprettetTid())).isPresent();
        }
        return false;
    }

    private boolean sjekkOmHendelseHarSammeVerdiOgErSendt(HendelsePayload payload,
                                                          Optional<InngåendeHendelse> tidligereHendelse,
                                                          Function<String, HendelsePayload> payloadFraJsonString) {
        if (tidligereHendelse.isEmpty()) {
            return false;
        } else if (tidligereHendelse.get().erSendtTilFpsak()) {
            boolean harSammeDato = payload.getHendelseDato().isPresent() && payload.getHendelseDato()
                .equals(payloadFraJsonString.apply(tidligereHendelse.get().getPayload()).getHendelseDato());
            if (harSammeDato) {
                LOGGER.info(
                    "Hendelse {} av type {} vil bli forkastet da endringstypen er KORRIGERT, uten at datoen {} er endret siden hendelse {}, som er forrige hendelse som ble sendt til FPSAK",
                    payload.getHendelseId(), payload.getHendelseType(), payload.getHendelseDato(), tidligereHendelse.get().getHendelseId());
            }
            return harSammeDato;
        } else if (tidligereHendelse.get().erFerdigbehandletMenIkkeSendtTilFpsak() && tidligereHendelse.get().getTidligereHendelseId() == null) {
            LOGGER.info("Hendelse {} av type {} beholdes da endringstypen er {}, uten at vi vet om vi har sendt tidligere hendelse {}",
                payload.getHendelseId(), payload.getHendelseType(), payload.getEndringstype(), tidligereHendelse.get().getHendelseId());
            return false;
        } else if (tidligereHendelse.get().erFerdigbehandletMenIkkeSendtTilFpsak() && tidligereHendelse.get().getTidligereHendelseId() != null) {
            // Gjøre sammenlikningen mot neste tidligere hendelse i stedet, i tilfelle den er sendt til Fpsak
            var nesteTidligereHendelse = getTidligereHendelse(tidligereHendelse.get().getTidligereHendelseId());
            return nesteTidligereHendelse.isPresent() && sjekkOmHendelseHarSammeVerdiOgErSendt(payload, nesteTidligereHendelse, payloadFraJsonString);
        }
        return false;
    }

    private Optional<InngåendeHendelse> getTidligereHendelse(String tidligereHendelseId) {
        return Optional.ofNullable(tidligereHendelseId).flatMap(th -> hendelseRepository.finnHendelseFraIdHvisFinnes(th, HendelseKilde.PDL));
    }

    /**
     * Identer inneholder flere identer for en person. fnr/dnr, aktørid osv.
     * Vi må hente ut det som er fnr fra dette settet.
     * <p>
     * En person kan ha flere fødselsnumre, gjerne der en av dem er et D-nummer.
     *
     * @param identer liste av forskjellige identer for en person(fnr, dnr, aktørid).
     * @return Liste av fødselsnumre, normalt bare en
     */
    public static Set<PersonIdent> hentUtFødselsnumreFraString(Set<String> identer) {
        if (Objects.isNull(identer)) {
            return null; // NOSONAR - ønsker ikke å returnere tomt Set, for da må man sjekke !isEmpty() i tillegg til isPresent()
        }

        return identer.stream().filter(PersonIdent::erGyldigFnr).map(PersonIdent::new).collect(Collectors.toSet());
    }

    /**
     * Identer inneholder flere identer for en person. fnr/dnr, aktørid osv.
     * Vi må hente ut det som er aktørId fra dette settet.
     * <p>
     * En person kan ha flere aktørIder, gjerne der en av dem er knyttet til et D-nummer.
     *
     * @param identer liste av forskjellige identer for en person(fnr, dnr, aktørid).
     * @return Liste av AktørId, normalt bare en
     */
    public static Set<String> hentUtAktørIderFraString(Set<String> identer, String hendelseId) {
        if (Objects.isNull(identer)) {
            return null; // NOSONAR - ønsker ikke å returnere tomt Set, for da må man sjekke !isEmpty() i tillegg til isPresent()
        }

        Set<String> aktørIder = identer.stream().filter(HendelseTjenesteHjelper::erAktørId).collect(Collectors.toSet());

        validerResultat(hendelseId, aktørIder);

        return aktørIder;
    }

    private static boolean erAktørId(String string) {
        return string != null && string.length() == 13 && string.matches("\\d+");
    }

    private static void validerResultat(String hendelseId, Set<String> aktørIder) {
        if (aktørIder.isEmpty()) {
            LOGGER.warn(AbonnentHendelserFeil.finnerIngenAktørId(hendelseId).getMessage());
        }
        if (aktørIder.size() > 1) {
            LOGGER.info(AbonnentHendelserFeil.merEnnEnAktørId(aktørIder.size(), hendelseId).getMessage());
        }
    }
}
