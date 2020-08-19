package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.tjeneste.AbonnentHendelserFeil;

public class TpsHendelseHjelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TpsHendelseHjelper.class);

    private TpsHendelseHjelper() {
        // Skal ikke konstrueres
    }

    /**
     * Identer inneholder flere identer for en person. fnr/dnr, aktørid osv.
     * Vi må hente ut det som er aktørId fra dette settet.
     *
     * En person kan ha flere aktørIder, gjerne der en av dem er knyttet til et D-nummer.
     *
     * @param identer liste av forskjellige identer for en person(fnr, dnr, aktørid).
     * @return Liste av AktørId, normalt bare en
     */
    public static Set<String> hentUtAktørIderFraString(Set<String> identer, String hendelseId) {
        if (Objects.isNull(identer)) {
            return null; // NOSONAR - ønsker ikke å returnere tomt Set, for da må man sjekke !isEmpty() i tillegg til isPresent()
        }

        Set<String> aktørIder = identer.stream()
                .filter(TpsHendelseHjelper::erAktørId)
                .collect(Collectors.toSet());

        validerResultat(hendelseId, aktørIder);

        return aktørIder;
    }

    private static boolean erAktørId(String string) {
        return string != null && string.length() == 13 && string.matches("\\d+");
    }

    private static void validerResultat(String hendelseId, Set<String> aktørIder) {
        if (aktørIder.isEmpty()) {
            AbonnentHendelserFeil.FACTORY.finnerIngenAktørId(hendelseId).log(LOGGER);
        }
        if (aktørIder.size() > 1) {
            AbonnentHendelserFeil.FACTORY.merEnnEnAktørId(aktørIder.size(), hendelseId).log(LOGGER);
        }
    }
}
