package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

public class PdlFalskIdentitet extends PdlPersonhendelse {

    private boolean erFalsk;

    public boolean erRelevantForFpsak() {
        // Kan se bort fra Opprettet + ikke-falsk. Ellers logg tilfelle
        return !PdlEndringstype.OPPRETTET.equals(super.getEndringstype()) || erFalsk;
    }

    public boolean getErFalsk() {
        return erFalsk;
    }

    public void setErFalsk(boolean erFalsk) {
        this.erFalsk = erFalsk;
    }

    @Override
    public String toString() {
        return "PdlUtflytting{" + toStringInnhold() + ", erFalsek=" + erFalsk + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlFalskIdentitet();
        }

        public Builder medErFalsk(boolean erFalsk) {
            getMal().erFalsk = erFalsk;
            return this;
        }

        public PdlFalskIdentitet build() {
            return getMal();
        }

        private PdlFalskIdentitet getMal() {
            return (PdlFalskIdentitet) mal;
        }
    }
}
