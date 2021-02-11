package no.nav.foreldrepenger.abonnent.felles.domene;

public class KlarForSorteringResultat {
    private boolean resultat;
    private boolean prøveIgjen;

    public KlarForSorteringResultat(boolean resultat) {
        this.resultat = resultat;
        this.prøveIgjen = false;
    }

    public KlarForSorteringResultat(boolean resultat, boolean prøveIgjen) {
        this.resultat = resultat;
        this.prøveIgjen = prøveIgjen;
    }

    public boolean hendelseKlarForSortering() {
        return resultat;
    }

    public boolean skalPrøveIgjen() {
        return prøveIgjen;
    }
}
