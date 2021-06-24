package no.nav.foreldrepenger.abonnent.pdl.domene.internt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.UtflyttingHendelseDto;

public class PdlUtflyttingHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate utflyttingsdato;

    public PdlUtflyttingHendelsePayload() {
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        var dto = new UtflyttingHendelseDto();
        dto.setId(UtflyttingHendelseDto.HENDELSE_TYPE + "_" + getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørId(finnAktørId(this));
        this.getUtflyttingsdato().ifPresent(dto::setUtflyttingsdato);
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørId(PdlUtflyttingHendelsePayload payload) {
        if (payload.getAktørId().isPresent()) {
            return payload.getAktørId().get().stream().map(AktørIdDto::new).collect(Collectors.toList());
        }
        return List.of();
    }

    public Optional<Set<String>> getAktørId() {
        return Optional.ofNullable(aktørId);
    }

    public Optional<LocalDate> getUtflyttingsdato() {
        return Optional.ofNullable(utflyttingsdato);
    }

    @Override
    public Optional<LocalDate> getHendelseDato() {
        return getUtflyttingsdato();
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PdlUtflyttingHendelsePayload payload = (PdlUtflyttingHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null) return false;
        if (tidligereHendelseId != null ? !tidligereHendelseId.equals(payload.tidligereHendelseId) : payload.tidligereHendelseId != null) return false;
        if (hendelseType != null ? !hendelseType.equals(payload.hendelseType) : payload.hendelseType != null) return false;
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) return false;
        if (hendelseOpprettetTid != null ? !hendelseOpprettetTid.equals(payload.hendelseOpprettetTid) : payload.hendelseOpprettetTid != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return utflyttingsdato != null ? utflyttingsdato.equals(payload.utflyttingsdato) : payload.utflyttingsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (tidligereHendelseId != null ? tidligereHendelseId.hashCode() : 0);
        result = 31 * result + (hendelseType != null ? hendelseType.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (utflyttingsdato != null ? utflyttingsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private PdlUtflyttingHendelsePayload kladd = new PdlUtflyttingHendelsePayload();

        public Builder hendelseId(String hendelseId) {
            this.kladd.hendelseId = hendelseId;
            return this;
        }

        public Builder tidligereHendelseId(String tidligereHendelseId) {
            this.kladd.tidligereHendelseId = tidligereHendelseId;
            return this;
        }

        public Builder hendelseType(String hendelseType) {
            this.kladd.hendelseType = hendelseType;
            return this;
        }

        public Builder endringstype(String endringstype) {
            this.kladd.endringstype = endringstype;
            return this;
        }

        public Builder hendelseOpprettetTid(LocalDateTime hendelseOpprettetTid) {
            this.kladd.hendelseOpprettetTid = hendelseOpprettetTid;
            return this;
        }

        public Builder aktørId(Set<String> aktørId) {
            this.kladd.aktørId = aktørId;
            return this;
        }

        public Builder utflyttingsdato(LocalDate utflyttingsdato) {
            this.kladd.utflyttingsdato = utflyttingsdato;
            return this;
        }

        public PdlUtflyttingHendelsePayload build() {
            return kladd;
        }
    }
}