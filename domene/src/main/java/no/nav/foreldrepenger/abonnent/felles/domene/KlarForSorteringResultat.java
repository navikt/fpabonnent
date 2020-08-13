package no.nav.foreldrepenger.abonnent.felles.domene;

public class KlarForSorteringResultat {
    private boolean resultat;

    public KlarForSorteringResultat(boolean resultat) {
        this.resultat = resultat;
    }

    public boolean hendelseKlarForSortering() {
        return resultat;
    }
}
