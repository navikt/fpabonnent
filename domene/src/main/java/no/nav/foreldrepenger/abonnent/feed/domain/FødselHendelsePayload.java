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
import no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto;

public class FødselHendelsePayload extends HendelsePayload {

    private Set<String> aktørIdMor;

    private Set<String> aktørIdFar;

    private Set<String> aktørIdBarn;

    private LocalDate fødselsdato;

    public FødselHendelsePayload() {
    }

    private FødselHendelsePayload(Builder builder) {
        this.sekvensnummer = builder.sekvensnummer;
        this.type = builder.type;
        this.aktørIdMor = builder.aktørIdMor;
        this.aktørIdFar = builder.aktørIdFar;
        this.aktørIdBarn = builder.aktørIdBarn;
        this.fødselsdato = builder.fødselsdato;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        FødselHendelseDto dto = new FødselHendelseDto();
        dto.setId(HendelseMapper.FØDSEL_HENDELSE_TYPE + this.getSekvensnummer());
        dto.setAktørIdForeldre(finnAktørIdForeldre(this));
        this.getFødselsdato().ifPresent(dto::setFødselsdato);
        return HendelseWrapperDto.lagDto(dto);
    }

    private List<String> finnAktørIdForeldre(FødselHendelsePayload payload) {
        List<String> aktørIder = new LinkedList<>();
        payload.getAktørIdFar().ifPresent(aktørIder::addAll);
        payload.getAktørIdMor().ifPresent(aktørIder::addAll);
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

        if (sekvensnummer != null ? !sekvensnummer.equals(payload.sekvensnummer) : payload.sekvensnummer != null)
            return false;
        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (aktørIdMor != null ? !aktørIdMor.equals(payload.aktørIdMor) : payload.aktørIdMor != null) return false;
        if (aktørIdFar != null ? !aktørIdFar.equals(payload.aktørIdFar) : payload.aktørIdFar != null) return false;
        if (aktørIdBarn != null ? !aktørIdBarn.equals(payload.aktørIdBarn) : payload.aktørIdBarn != null) return false;
        return fødselsdato != null ? fødselsdato.equals(payload.fødselsdato) : payload.fødselsdato == null;
    }

    @Override
    public int hashCode() {
        int result = sekvensnummer != null ? sekvensnummer.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aktørIdMor != null ? aktørIdMor.hashCode() : 0);
        result = 31 * result + (aktørIdFar != null ? aktørIdFar.hashCode() : 0);
        result = 31 * result + (aktørIdBarn != null ? aktørIdBarn.hashCode() : 0);
        result = 31 * result + (fødselsdato != null ? fødselsdato.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private Long sekvensnummer;
        private String type;
        private Set<String> aktørIdMor;
        private Set<String> aktørIdFar;
        private Set<String> aktørIdBarn;
        private LocalDate fødselsdato;

        public Builder sekvensnummer(Long sekvensnummer) {
            this.sekvensnummer = sekvensnummer;
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
