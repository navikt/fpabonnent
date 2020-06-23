package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
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

public class DødHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødsdato;

    public DødHendelsePayload() {
    }

    private DødHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.type = builder.type;
        this.endringstype = "OPPRETTET";
        this.aktørId = builder.aktørId;
        this.dødsdato = builder.dødsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødHendelseDto dto = new DødHendelseDto();
        dto.setId(HendelseMapper.DØD_HENDELSE_TYPE + this.getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørId(finnAktørId(this));
        this.getDødsdato().ifPresent(dto::setDødsdato);
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørId(DødHendelsePayload payload) {
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
        return FeedKode.TPS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DødHendelsePayload payload = (DødHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return dødsdato != null ? dødsdato.equals(payload.dødsdato) : payload.dødsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødsdato != null ? dødsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String type;
        private Set<String> aktørId;
        private LocalDate dødsdato;

        public DødHendelsePayload.Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public DødHendelsePayload.Builder type(String type) {
            this.type = type;
            return this;
        }

        public DødHendelsePayload.Builder aktørId(Set<String> aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public DødHendelsePayload.Builder dødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public DødHendelsePayload build() {
            return new DødHendelsePayload(this);
        }
    }
}