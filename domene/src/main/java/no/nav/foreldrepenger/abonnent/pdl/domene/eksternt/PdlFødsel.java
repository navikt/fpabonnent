package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

import java.time.LocalDate;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

public class PdlFødsel extends PdlPersonhendelse {

    private LocalDate fødselsdato;
    private Set<String> aktørIdForeldre;

    public boolean erRelevantForFpsak() {
        return HendelseType.PDL_FØDSEL_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_FØDSEL_ANNULLERT.equals(getHendelseType())
                || HendelseType.PDL_FØDSEL_KORRIGERT.equals(getHendelseType());
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Set<String> getAktørIdForeldre() {
        return aktørIdForeldre;
    }

    public void setAktørIdForeldre(Set<String> aktørIdForeldre) {
        this.aktørIdForeldre = aktørIdForeldre;
    }

    @Override
    public String toString() {
        return "PdlFødsel{" + toStringInnhold() +
                ", fødselsdato=" + fødselsdato +
                ", aktørIdForeldre=" + aktørIdForeldre +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlFødsel();
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            getMal().fødselsdato = fødselsdato;
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
