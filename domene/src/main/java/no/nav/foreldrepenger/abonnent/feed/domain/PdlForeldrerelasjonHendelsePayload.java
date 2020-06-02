package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødHendelseDto;

public class PdlForeldrerelasjonHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    public PdlForeldrerelasjonHendelsePayload() {
    }

    private PdlForeldrerelasjonHendelsePayload(Builder builder) {
        this.hendelseId = builder.hendelseId;
        this.type = builder.type;
        this.endringstype = builder.endringstype;
        this.hendelseOpprettetTid = builder.hendelseOpprettetTid;
        this.aktørId = builder.aktørId;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        DødHendelseDto dto = new DødHendelseDto();
        dto.setId(HendelseMapper.FORELDRERELASJON_HENDELSE_TYPE + "_" + getHendelseId());
        dto.setAktørId(finnAktørId(this));
        return HendelseWrapperDto.lagDto(dto);
    }

    private List<String> finnAktørId(PdlForeldrerelasjonHendelsePayload payload) {
        List<String> aktørIder = new LinkedList<>();
        payload.getAktørId().ifPresent(aktørIder::addAll);
        return aktørIder;
    }

    public Optional<Set<String>> getAktørId() {
        return Optional.ofNullable(aktørId);
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

        PdlForeldrerelasjonHendelsePayload payload = (PdlForeldrerelasjonHendelsePayload) o;

        if (type != null ? !type.equals(payload.type) : payload.type != null) return false;
        if (endringstype != null ? !endringstype.equals(payload.endringstype) : payload.endringstype != null) return false;
        if (hendelseOpprettetTid != null ? !hendelseOpprettetTid.equals(payload.hendelseOpprettetTid) : payload.hendelseOpprettetTid != null) return false;
        return aktørId != null ? !aktørId.equals(payload.aktørId) : payload.aktørId != null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String hendelseId;
        private String type;
        private String endringstype;
        private LocalDateTime hendelseOpprettetTid;
        private Set<String> aktørId;

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

        public Builder hendelseOpprettetTid(LocalDateTime hendelseOpprettetTid) {
            this.hendelseOpprettetTid = hendelseOpprettetTid;
            return this;
        }

        public Builder aktørId(Set<String> aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public PdlForeldrerelasjonHendelsePayload build() {
            return new PdlForeldrerelasjonHendelsePayload(this);
        }
    }
}