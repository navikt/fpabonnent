package no.nav.foreldrepenger.abonnent.pdl.domene.internt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;

public class PdlFødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørIdBarn;

    private Set<String> aktørIdForeldre;

    private LocalDate fødselsdato;

    public PdlFødselHendelsePayload() {
    }

    private PdlFødselHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.tidligereHendelseId = builder.tidligereHendelseId;
        this.hendelseType = builder.hendelseType;
        this.endringstype = builder.endringstype;
        this.hendelseOpprettetTid = builder.hendelseOpprettetTid;
        this.aktørIdBarn = builder.aktørIdBarn;
        this.aktørIdForeldre = builder.aktørIdForeldre;
        this.fødselsdato = builder.fødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        FødselHendelseDto dto = new FødselHendelseDto();
        dto.setId(FødselHendelseDto.HENDELSE_TYPE + "_" + getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        this.getFødselsdato().ifPresent(dto::setFødselsdato);
        this.getAktørIdForeldre().ifPresent(foreldre -> dto.setAktørIdForeldre(foreldre.stream().map(AktørIdDto::new).collect(Collectors.toList())));
        return new HendelseWrapperDto(dto);
    }

    public Optional<Set<String>> getAktørIdBarn() {
        return Optional.ofNullable(aktørIdBarn);
    }

    public Optional<Set<String>> getAktørIdForeldre() {
        return Optional.ofNullable(aktørIdForeldre);
    }

    public Optional<LocalDate> getFødselsdato() {
        return Optional.ofNullable(fødselsdato);
    }

    @Override
    public Optional<LocalDate> getHendelseDato() {
        return getFødselsdato();
    }

    @Override
    public Set<String> getAktørIderForSortering() {
        Set<String> set = new HashSet<>();

        if (aktørIdBarn != null) {
            set.addAll(aktørIdBarn);
        }

        if (aktørIdForeldre != null) {
            set.addAll(aktørIdForeldre);
        }

        return set;
    }

    @Override
    public HendelseKilde getHendelseKilde() {
        return HendelseKilde.PDL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null) return false;
        if (tidligereHendelseId != null ? !tidligereHendelseId.equals(payload.tidligereHendelseId) : payload.tidligereHendelseId != null) return false;
        if (hendelseType != null ? !hendelseType.equals(payload.hendelseType) : payload.hendelseType != null) return false;
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) return false;
        if (hendelseOpprettetTid != null ? !hendelseOpprettetTid.equals(payload.hendelseOpprettetTid) : payload.hendelseOpprettetTid != null) return false;
        if (aktørIdBarn != null ? !aktørIdBarn.equals(payload.aktørIdBarn) : payload.aktørIdBarn != null) return false;
        if (aktørIdForeldre != null ? !aktørIdForeldre.equals(payload.aktørIdForeldre) : payload.aktørIdForeldre != null) return false;
        return fødselsdato != null ? fødselsdato.equals(payload.fødselsdato) : payload.fødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (tidligereHendelseId != null ? tidligereHendelseId.hashCode() : 0);
        result = 31 * result + (hendelseType != null ? hendelseType.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørIdBarn != null ? aktørIdBarn.hashCode() : 0);
        result = 31 * result + (aktørIdForeldre != null ? aktørIdForeldre.hashCode() : 0);
        result = 31 * result + (fødselsdato != null ? fødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String tidligereHendelseId;
        private String hendelseType;
        private String endringstype;
        private LocalDateTime hendelseOpprettetTid;
        private Set<String> aktørIdBarn;
        private Set<String> aktørIdForeldre;
        private LocalDate fødselsdato;

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

        public Builder aktørIdBarn(Set<String> aktørIdBarn) {
            this.aktørIdBarn = aktørIdBarn;
            return this;
        }

        public Builder aktørIdForeldre(Set<String> aktørIdForeldre) {
            this.aktørIdForeldre = aktørIdForeldre;
            return this;
        }

        public Builder fødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public PdlFødselHendelsePayload build() {
            return new PdlFødselHendelsePayload(this);
        }
    }
}
