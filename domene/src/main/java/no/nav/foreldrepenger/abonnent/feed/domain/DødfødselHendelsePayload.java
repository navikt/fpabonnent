package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.FeedKode;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto;

public class DødfødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødfødselsdato;

    public DødfødselHendelsePayload() {
    }

    private DødfødselHendelsePayload(DødfødselHendelsePayload.Builder builder) {
        this.sekvensnummer = builder.sekvensnummer;
        this.type = builder.type;
        this.aktørId = builder.aktørId;
        this.dødfødselsdato = builder.dødfødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødfødselHendelseDto dto = new DødfødselHendelseDto();
        dto.setId(HendelseMapper.DØDFØDSEL_HENDELSE_TYPE + this.getSekvensnummer());
        dto.setAktørId(finnAktørId(this));
        this.getDødfødselsdato().ifPresent(dto::setDødfødselsdato);
        return HendelseWrapperDto.lagDto(dto);
    }

    private List<String> finnAktørId(DødfødselHendelsePayload payload) {
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

    public Optional<LocalDate> getDødfødselsdato() {
        return Optional.ofNullable(dødfødselsdato);
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

        DødfødselHendelsePayload payload = (DødfødselHendelsePayload) o;

        if (sekvensnummer != null ? !sekvensnummer.equals(payload.sekvensnummer) : payload.sekvensnummer != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return dødfødselsdato != null ? dødfødselsdato.equals(payload.dødfødselsdato) : payload.dødfødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = sekvensnummer != null ? sekvensnummer.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødfødselsdato != null ? dødfødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private Long sekvensnummer;
        private String type;
        private Set<String> aktørId;
        private LocalDate dødfødselsdato;

        public DødfødselHendelsePayload.Builder sekvensnummer(Long sekvensnummer) {
            this.sekvensnummer = sekvensnummer;
            return this;
        }

        public DødfødselHendelsePayload.Builder type(String type) {
            this.type = type;
            return this;
        }

        public DødfødselHendelsePayload.Builder aktørId(Set<String> aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public DødfødselHendelsePayload.Builder dødfødselsdato(LocalDate dødfødselsdato) {
            this.dødfødselsdato = dødfødselsdato;
            return this;
        }

        public DødfødselHendelsePayload build() {
            return new DødfødselHendelsePayload(this);
        }
    }
}