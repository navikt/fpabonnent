package no.nav.foreldrepenger.abonnent.pdl.domene;

import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;

public class PdlFamilierelasjon extends PdlPersonhendelse {

    private String relatertPersonsIdent;
    private String relatertPersonsRolle;
    private String minRolleForPerson;

    public boolean erRelevantForFpsak() {
        return (HendelseType.PDL_FAMILIERELASJON_OPPRETTET.equals(getHendelseType())
                || HendelseType.PDL_FAMILIERELASJON_ANNULLERT.equals(getHendelseType()))
                && "BARN".contains(getRelatertPersonsRolle());
    }

    public String getRelatertPersonsIdent() {
        return relatertPersonsIdent;
    }

    public String getRelatertPersonsRolle() {
        return relatertPersonsRolle;
    }

    public String getMinRolleForPerson() {
        return minRolleForPerson;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends PdlPersonhendelseBuilder {

        public Builder() {
            super.mal = new PdlFamilierelasjon();
        }

        public Builder medRelatertPersonsIdent(String relatertPersonsIdent) {
            getMal().relatertPersonsIdent = relatertPersonsIdent;
            return this;
        }

        public Builder medRelatertPersonsRolle(String relatertPersonsRolle) {
            getMal().relatertPersonsRolle = relatertPersonsRolle;
            return this;
        }

        public Builder medMinRolleForPerson(String minRolleForPerson) {
            getMal().minRolleForPerson = minRolleForPerson;
            return this;
        }

        public PdlFamilierelasjon build() {
            return getMal();
        }

        private PdlFamilierelasjon getMal() {
            return (PdlFamilierelasjon) mal;
        }
    }
}
