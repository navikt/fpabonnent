package no.nav.foreldrepenger.abonnent.pdl.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;

public class PdlDødfødsel extends PdlPersonhendelse {

    private LocalDate dødfødselsdato;

    public boolean erRelevantForFpsak() {
        return (HendelseType.PDL_DØDFØDSEL_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_DØDFØDSEL_ANNULLERT.equals(getHendelseType())
                || HendelseType.PDL_DØDFØDSEL_KORRIGERT.equals(getHendelseType()));
    }

    public LocalDate getDødfødselsdato() {
        return dødfødselsdato;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlDødfødsel();
        }

        public Builder medDødfødselsdato(LocalDate dødfødselsdato) {
            getMal().dødfødselsdato = dødfødselsdato;
            return this;
        }

        public PdlDødfødsel build() {
            return getMal();
        }

        private PdlDødfødsel getMal() {
            return (PdlDødfødsel) mal;
        }
    }
}
