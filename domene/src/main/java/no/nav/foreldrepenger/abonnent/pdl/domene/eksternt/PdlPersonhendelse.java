package no.nav.foreldrepenger.abonnent.pdl.domene.eksternt;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;

public abstract class PdlPersonhendelse {

    private String hendelseId;
    private Set<String> personidenter = new HashSet<>();
    private String master;
    private LocalDateTime opprettet;
    private String opplysningstype;
    private PdlEndringstype endringstype;
    private String tidligereHendelseId;
    private HendelseType hendelseType;

    public abstract boolean erRelevantForFpsak();

    public String getHendelseId() {
        return hendelseId;
    }

    public Set<String> getPersonidenter() {
        return personidenter;
    }

    public String getMaster() {
        return master;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public String getOpplysningstype() {
        return opplysningstype;
    }

    public PdlEndringstype getEndringstype() {
        return endringstype;
    }

    public String getTidligereHendelseId() {
        return tidligereHendelseId;
    }

    public HendelseType getHendelseType() {
        return hendelseType;
    }

    protected String toStringInnhold() {
        return "hendelseId='" + hendelseId + '\'' +
                ", personidenter=" + personidenter +
                ", master='" + master + '\'' +
                ", opprettet=" + opprettet +
                ", opplysningstype='" + opplysningstype + '\'' +
                ", endringstype=" + endringstype +
                ", tidligereHendelseId='" + tidligereHendelseId + '\'' +
                ", hendelseType=" + hendelseType;
    }

    public static abstract class PdlPersonhendelseBuilder {
        protected PdlPersonhendelse mal;

        public PdlPersonhendelseBuilder medHendelseId(String hendelseId) {
            mal.hendelseId = hendelseId;
            return this;
        }

        public PdlPersonhendelseBuilder leggTilPersonident(String personident) {
            mal.personidenter.add(personident);
            return this;
        }

        public PdlPersonhendelseBuilder medMaster(String master) {
            mal.master = master;
            return this;
        }

        public PdlPersonhendelseBuilder medOpprettet(LocalDateTime opprettet) {
            mal.opprettet = opprettet;
            return this;
        }

        public PdlPersonhendelseBuilder medOpplysningstype(String opplysningstype) {
            mal.opplysningstype = opplysningstype;
            return this;
        }

        public PdlPersonhendelseBuilder medEndringstype(PdlEndringstype endringstype) {
            mal.endringstype = endringstype;
            return this;
        }

        public PdlPersonhendelseBuilder medTidligereHendelseId(String tidligereHendelseId) {
            mal.tidligereHendelseId = tidligereHendelseId;
            return this;
        }

        public PdlPersonhendelseBuilder medHendelseType(HendelseType hendelseType) {
            mal.hendelseType = hendelseType;
            return this;
        }
    }
}
