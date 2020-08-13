package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

import java.time.LocalDate;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

public class PdlDød extends PdlPersonhendelse {

    private LocalDate dødsdato;

    public boolean erRelevantForFpsak() {
        return (HendelseType.PDL_DØD_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_DØD_ANNULLERT.equals(getHendelseType())
                || HendelseType.PDL_DØD_KORRIGERT.equals(getHendelseType()));
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlDød();
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            getMal().dødsdato = dødsdato;
            return this;
        }

        public PdlDød build() {
            return getMal();
        }

        private PdlDød getMal() {
            return (PdlDød) mal;
        }
    }
}
