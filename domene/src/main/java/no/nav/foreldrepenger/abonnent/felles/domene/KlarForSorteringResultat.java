package no.nav.foreldrepenger.abonnent.felles.domene;

public record KlarForSorteringResultat(boolean resultat, boolean prøveIgjen) {

    public KlarForSorteringResultat(boolean resultat) {
        this(resultat, false);
    }
}
