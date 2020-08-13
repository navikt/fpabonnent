package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

import java.time.LocalDate;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

public class PdlFødsel extends PdlPersonhendelse {

    private Integer fødselsår;
    private LocalDate fødselsdato;
    private String fødeland;
    private String fødested;
    private String fødekommune;
    private Set<String> aktørIdForeldre;

    public boolean erRelevantForFpsak() {
        return HendelseType.PDL_FØDSEL_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_FØDSEL_ANNULLERT.equals(getHendelseType())
                || HendelseType.PDL_FØDSEL_KORRIGERT.equals(getHendelseType());
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

    public Set<String> getAktørIdForeldre() {
        return aktørIdForeldre;
    }

    public void setAktørIdForeldre(Set<String> aktørIdForeldre) {
        this.aktørIdForeldre = aktørIdForeldre;
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
