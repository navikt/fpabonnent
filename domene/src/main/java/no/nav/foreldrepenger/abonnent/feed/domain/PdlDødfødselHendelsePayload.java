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
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto;

public class PdlDødfødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private LocalDate dødfødselsdato;

    public PdlDødfødselHendelsePayload() {
    }

    private PdlDødfødselHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.type = builder.type;
        this.endringstype = builder.endringstype;
        this.aktørId = builder.aktørId;
        this.dødfødselsdato = builder.dødfødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødfødselHendelseDto dto = new DødfødselHendelseDto();
        dto.setId(HendelseMapper.DØDFØDSEL_HENDELSE_TYPE + "_" + getHendelseId());
        dto.setAktørId(finnAktørId(this));
        this.getDødfødselsdato().ifPresent(dto::setDødfødselsdato);
        return HendelseWrapperDto.lagDto(dto);
    }

    private List<String> finnAktørId(PdlDødfødselHendelsePayload payload) {
        List<String> aktørIder = new LinkedList<>();
        payload.getAktørId().ifPresent(aktørIder::addAll);
        return aktørIder;
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
        return FeedKode.PDL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PdlDødfødselHendelsePayload payload = (PdlDødfødselHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) return false;
        if (aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null) return false;
        return dødfødselsdato != null ? dødfødselsdato.equals(payload.dødfødselsdato) : payload.dødfødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (dødfødselsdato != null ? dødfødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String type;
        private String endringstype;
        private Set<String> aktørId;
        private LocalDate dødfødselsdato;

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
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