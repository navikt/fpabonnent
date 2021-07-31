package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

import java.time.LocalDate;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

public class PdlUtflytting extends PdlPersonhendelse {

    // For innkommende hendelser er dette dato oppgitt av bruker. Må utledes dersom mangler.
    private LocalDate utflyttingsdato;

    public boolean erRelevantForFpsak() {
        return (HendelseType.PDL_UTFLYTTING_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_UTFLYTTING_ANNULLERT.equals(getHendelseType()));
    }

    public LocalDate getUtflyttingsdato() {
        return utflyttingsdato;
    }

    public void setUtflyttingsdato(LocalDate utflyttingsdato) {
        this.utflyttingsdato = utflyttingsdato;
    }

    @Override
    public String toString() {
        return "PdlUtflytting{" + toStringInnhold() +
                ", utflyttingsdato=" + utflyttingsdato +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlUtflytting();
        }

        public Builder medUtflyttingsdato(LocalDate utflyttingsdato) {
            getMal().utflyttingsdato = utflyttingsdato;
            return this;
        }

        public PdlUtflytting build() {
            return getMal();
        }

        private PdlUtflytting getMal() {
            return (PdlUtflytting) mal;
        }
    }
}
