package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødHendelseDto;

public class PdlDødHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødsdato;

    public PdlDødHendelsePayload() {
    }

    private PdlDødHendelsePayload(PdlDødHendelsePayload.Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.tidligereHendelseId = builder.tidligereHendelseId;
        this.type = builder.type;
        this.endringstype = builder.endringstype;
        this.hendelseOpprettetTid = builder.hendelseOpprettetTid;
        this.aktørId = builder.aktørId;
        this.dødsdato = builder.dødsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødHendelseDto dto = new DødHendelseDto();
        dto.setId(HendelseMapper.DØD_HENDELSE_TYPE + "_" + getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørId(finnAktørId(this));
        this.getDødsdato().ifPresent(dto::setDødsdato);
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørId(PdlDødHendelsePayload payload) {
        if (payload.getAktørId().isPresent()) {
            return payload.getAktørId().get().stream().map(AktørIdDto::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<Set<String>> getAktørId() {
        return Optional.ofNullable(aktørId);
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
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
    public boolean erAtomisk() {
        return true;
    }

    @Override
    public FeedKode getFeedKode() {
        return FeedKode.PDL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PdlDødHendelsePayload payload = (PdlDødHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null) return false;
        if (tidligereHendelseId != null ? !tidligereHendelseId.equals(payload.tidligereHendelseId) : payload.tidligereHendelseId != null) return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) return false;
        if (hendelseOpprettetTid != null ? !hendelseOpprettetTid.equals(payload.hendelseOpprettetTid) : payload.hendelseOpprettetTid != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return dødsdato != null ? dødsdato.equals(payload.dødsdato) : payload.dødsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (tidligereHendelseId != null ? tidligereHendelseId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødsdato != null ? dødsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String tidligereHendelseId;
        private String type;
        private String endringstype;
        private LocalDateTime hendelseOpprettetTid;
        private Set<String> aktørId;
        private LocalDate dødsdato;

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder tidligereHendelseId(String tidligereHendelseId) {
            this.tidligereHendelseId = tidligereHendelseId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
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

        public Builder dødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public PdlDødHendelsePayload build() {
            return new PdlDødHendelsePayload(this);
        }
    }
}