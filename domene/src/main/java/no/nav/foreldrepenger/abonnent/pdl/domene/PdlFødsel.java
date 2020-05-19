package no.nav.foreldrepenger.abonnent.pdl.domene;

import java.time.LocalDate;

import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;

public class PdlFødsel extends PdlPersonhendelse {

    private Integer fødselsår;
    private LocalDate fødselsdato;
    private String fødeland;
    private String fødested;
    private String fødekommune;

    public boolean erRelevantForFpsak() {
        return HendelseType.PDL_FØDSEL_ANNULLERT.equals(getHendelseType());
    }

    public Integer getFødselsår() {
        return fødselsår;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public String getFødeland() {
        return fødeland;
    }

    public String getFødested() {
        return fødested;
    }

    public String getFødekommune() {
        return fødekommune;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlFødsel();
        }

        public Builder medFødselsår(Integer fødselsår) {
            getMal().fødselsår = fødselsår;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            getMal().fødselsdato = fødselsdato;
            return this;
        }

        public Builder medFødeland(String fødeland) {
            getMal().fødeland = fødeland;
            return this;
        }

        public Builder medFødested(String fødested) {
            getMal().fødested = fødested;
            return this;
        }

        public Builder medFødekommune(String fødekommune) {
            getMal().fødekommune = fødekommune;
            return this;
        }

        public PdlFødsel build() {
            return getMal();
        }

        private PdlFødsel getMal() {
            return (PdlFødsel) mal;
        }
    }
}
