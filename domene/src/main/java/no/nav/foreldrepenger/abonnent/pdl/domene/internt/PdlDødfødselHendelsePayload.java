package no.nav.foreldrepenger.abonnent.pdl.domene.internt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;

public class PdlDødfødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødfødselsdato;

    public PdlDødfødselHendelsePayload() {
    }

    private PdlDødfødselHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.tidligereHendelseId = builder.tidligereHendelseId;
        this.hendelseType = builder.hendelseType;
        this.endringstype = builder.endringstype;
        this.hendelseOpprettetTid = builder.hendelseOpprettetTid;
        this.aktørId = builder.aktørId;
        this.dødfødselsdato = builder.dødfødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        var dto = new DødfødselHendelseDto();
        dto.setId(DødfødselHendelseDto.HENDELSE_TYPE + "_" + getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørId(finnAktørId(this));
        this.getDødfødselsdato().ifPresent(dto::setDødfødselsdato);
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørId(PdlDødfødselHendelsePayload payload) {
        return payload.getAktørId().map(ids -> ids.stream().map(AktørIdDto::new).toList()).orElseGet(List::of);
    }

    public Optional<Set<String>> getAktørId() {
        return Optional.ofNullable(aktørId);
    }

    public Optional<LocalDate> getDødfødselsdato() {
        return Optional.ofNullable(dødfødselsdato);
    }

    @Override
    public Optional<LocalDate> getHendelseDato() {
        return getDødfødselsdato();
    }

    @Override
    public Set<String> getAktørIderForSortering() {
        Set<String> set = new HashSet<>();
        if (aktørId != null) {
            set.addAll(aktørId);
        }
        return set;
    }

    @Override
    public HendelseKilde getHendelseKilde() {
        return HendelseKilde.PDL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var payload = (PdlDødfødselHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null) {
            return false;
        }
        if (tidligereHendelseId != null ? !tidligereHendelseId.equals(payload.tidligereHendelseId) : payload.tidligereHendelseId != null) {
            return false;
        }
        if (hendelseType != null ? !hendelseType.equals(payload.hendelseType) : payload.hendelseType != null) {
            return false;
        }
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) {
            return false;
        }
        if (hendelseOpprettetTid != null ? !hendelseOpprettetTid.equals(payload.hendelseOpprettetTid) : payload.hendelseOpprettetTid != null) {
            return false;
        }
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) {
            return false;
        }
        return dødfødselsdato != null ? dødfødselsdato.equals(payload.dødfødselsdato) : payload.dødfødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (tidligereHendelseId != null ? tidligereHendelseId.hashCode() : 0);
        result = 31 * result + (hendelseType != null ? hendelseType.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødfødselsdato != null ? dødfødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String tidligereHendelseId;
        private String hendelseType;
        private String endringstype;
        private LocalDateTime hendelseOpprettetTid;
        private Set<String> aktørId;
        private LocalDate dødfødselsdato;

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder tidligereHendelseId(String tidligereHendelseId) {
            this.tidligereHendelseId = tidligereHendelseId;
            return this;
        }

        public Builder hendelseType(String hendelseType) {
            this.hendelseType = hendelseType;
            return this;
        }

        public Builder endringstype(String endringstype) {
            this.endringstype = endringstype;
            return this;
        }

        public Builder hendelseOpprettetTid(LocalDateTime hendelseOpprettetTid) {
            this.hendelseOpprettetTid = hendelseOpprettetTid;
            return this;
        }

        public Builder aktørId(Set<String> aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder dødfødselsdato(LocalDate dødfødselsdato) {
            this.dødfødselsdato = dødfødselsdato;
            return this;
        }

        public PdlDødfødselHendelsePayload build() {
            return new PdlDødfødselHendelsePayload(this);
        }
    }
}
