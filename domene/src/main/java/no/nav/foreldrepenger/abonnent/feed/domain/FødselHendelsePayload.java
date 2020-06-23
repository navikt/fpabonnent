package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;

public class FødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørIdMor;

    private Set<String> aktørIdFar;

    private Set<String> aktørIdBarn;

    private LocalDate fødselsdato;

    public FødselHendelsePayload() {
    }

    private FødselHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.type = builder.type;
        this.endringstype = "OPPRETTET";
        this.aktørIdMor = builder.aktørIdMor;
        this.aktørIdFar = builder.aktørIdFar;
        this.aktørIdBarn = builder.aktørIdBarn;
        this.fødselsdato = builder.fødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        FødselHendelseDto dto = new FødselHendelseDto();
        dto.setId(HendelseMapper.FØDSEL_HENDELSE_TYPE + this.getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørIdForeldre(finnAktørIdForeldre(this));
        this.getFødselsdato().ifPresent(dto::setFødselsdato);
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørIdForeldre(FødselHendelsePayload payload) {
        List<AktørIdDto> aktørIder = new LinkedList<>();
        if (payload.getAktørIdFar().isPresent()) {
            aktørIder.addAll(payload.getAktørIdFar().get().stream().map(AktørIdDto::new).collect(Collectors.toList()));
        }
        if (payload.getAktørIdMor().isPresent()) {
            aktørIder.addAll(payload.getAktørIdMor().get().stream().map(AktørIdDto::new).collect(Collectors.toList()));
        }
        return aktørIder;
    }

    public Optional<Set<String>> getAktørIdMor() {
        return Optional.ofNullable(aktørIdMor);
    }

    public Optional<Set<String>> getAktørIdFar() {
        return Optional.ofNullable(aktørIdFar);
    }

    public Optional<Set<String>> getAktørIdBarn() {
        return Optional.ofNullable(aktørIdBarn);
    }

    public Optional<LocalDate> getFødselsdato() {
        return Optional.ofNullable(fødselsdato);
    }

    @Override
    public Set<String> getAktørIderForSortering() {
        Set<String> set = new HashSet<>();
        if (aktørIdFar != null) {
            set.addAll(aktørIdFar);
        }

        if (aktørIdMor != null) {
            set.addAll(aktørIdMor);
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

        FødselHendelsePayload payload = (FødselHendelsePayload) o;

        if (hendelseId != null ? !hendelseId.equals(payload.hendelseId) : payload.hendelseId != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (aktørIdMor != null ? !aktørIdMor.equals(payload.aktørIdMor) : payload.aktørIdMor != null) return false;
        if (aktørIdFar != null ? !aktørIdFar.equals(payload.aktørIdFar) : payload.aktørIdFar != null) return false;
        if (aktørIdBarn != null ? !aktørIdBarn.equals(payload.aktørIdBarn) : payload.aktørIdBarn != null) return false;
        return fødselsdato != null ? fødselsdato.equals(payload.fødselsdato) : payload.fødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aktørIdMor != null ? aktørIdMor.hashCode() : 0);
        result = 31 * result + (aktørIdFar != null ? aktørIdFar.hashCode() : 0);
        result = 31 * result + (aktørIdBarn != null ? aktørIdBarn.hashCode() : 0);
        result = 31 * result + (fødselsdato != null ? fødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String type;
        private Set<String> aktørIdMor;
        private Set<String> aktørIdFar;
        private Set<String> aktørIdBarn;
        private LocalDate fødselsdato;

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder aktørIdMor(Set<String> aktørIdMor) {
            this.aktørIdMor = aktørIdMor;
            return this;
        }

        public Builder aktørIdFar(Set<String> aktørIdFar) {
            this.aktørIdFar = aktørIdFar;
            return this;
        }

        public Builder aktørIdBarn(Set<String> aktørIdBarn) {
            this.aktørIdBarn = aktørIdBarn;
            return this;
        }

        public Builder fødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public FødselHendelsePayload build() {
            return new FødselHendelsePayload(this);
        }
    }
}
