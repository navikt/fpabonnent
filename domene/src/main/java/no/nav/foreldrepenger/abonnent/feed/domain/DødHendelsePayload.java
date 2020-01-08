package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto;

public class DødHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødsdato;

    public DødHendelsePayload() {
    }

    private DødHendelsePayload(DødHendelsePayload.Builder builder) {
        this.sekvensnummer = builder.sekvensnummer;
        this.type = builder.type;
        this.aktørId = builder.aktørId;
        this.dødsdato = builder.dødsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødHendelseDto dto = new DødHendelseDto();
        dto.setId(HendelseMapper.DØD_HENDELSE_TYPE + this.getSekvensnummer());
        dto.setAktørId(finnAktørId(this));
        this.getDødsdato().ifPresent(dto::setDødsdato);
        return HendelseWrapperDto.lagDto(dto);
    }

    private List<String> finnAktørId(DødHendelsePayload payload) {
        List<String> aktørIder = new LinkedList<>();
        payload.getAktørId().ifPresent(aktørIder::addAll);
        return aktørIder;
    }

    @Override
    public Long getSekvensnummer() {
        return sekvensnummer;
    }

    @Override
    public String getType() {
        return type;
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

        if (sekvensnummer != null ? !sekvensnummer.equals(payload.sekvensnummer) : payload.sekvensnummer != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return dødsdato != null ? dødsdato.equals(payload.dødsdato) : payload.dødsdato == null;
    }

    @Override
    public int hashCode() {
        int result = sekvensnummer != null ? sekvensnummer.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødsdato != null ? dødsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private Long sekvensnummer;
        private String type;
        private Set<String> aktørId;
        private LocalDate dødsdato;

        public DødHendelsePayload.Builder sekvensnummer(Long sekvensnummer) {
            this.sekvensnummer = sekvensnummer;
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