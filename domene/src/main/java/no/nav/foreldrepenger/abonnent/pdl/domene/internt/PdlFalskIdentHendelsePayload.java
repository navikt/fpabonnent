package no.nav.foreldrepenger.abonnent.pdl.domene.internt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelsePayload;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FalskIdentitetHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.UtflyttingHendelseDto;
import no.nav.vedtak.konfig.Tid;

public class PdlFalskIdentHendelsePayload extends HendelsePayload {

    private Set<String> aktørId;

    private boolean erFalsk;

    public PdlFalskIdentHendelsePayload() {
        // CDI
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {
        var dto = new FalskIdentitetHendelseDto();
        dto.setId(UtflyttingHendelseDto.HENDELSE_TYPE + "_" + getHendelseId());
        dto.setEndringstype(Endringstype.valueOf(endringstype));
        dto.setAktørId(finnAktørId(this));
        dto.setErFalsk(getErFalsk());
        return new HendelseWrapperDto(dto);
    }

    private List<AktørIdDto> finnAktørId(PdlFalskIdentHendelsePayload payload) {
        return payload.getAktørId().map(strings -> strings.stream().map(AktørIdDto::new).toList()).orElseGet(List::of);
    }

    public Optional<Set<String>> getAktørId() {
        return Optional.ofNullable(aktørId);
    }

    public boolean getErFalsk() {
        return erFalsk;
    }

    @Override
    public Optional<LocalDate> getHendelseDato() {
        return Optional.of(Tid.TIDENES_BEGYNNELSE);
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var payload = (PdlFalskIdentHendelsePayload) o;

        if (!Objects.equals(hendelseId, payload.hendelseId)) {
            return false;
        }
        if (!Objects.equals(tidligereHendelseId, payload.tidligereHendelseId)) {
            return false;
        }
        if (!Objects.equals(hendelseType, payload.hendelseType)) {
            return false;
        }
        if (!Objects.equals(endringstype, payload.endringstype)) {
            return false;
        }
        if (!Objects.equals(hendelseOpprettetTid, payload.hendelseOpprettetTid)) {
            return false;
        }
        if (!Objects.equals(aktørId, payload.aktørId)) {
            return false;
        }
        return Objects.equals(erFalsk, payload.erFalsk);
    }

    @Override
    public int hashCode() {
        int result = hendelseId != null ? hendelseId.hashCode() : 0;
        result = 31 * result + (tidligereHendelseId != null ? tidligereHendelseId.hashCode() : 0);
        result = 31 * result + (hendelseType != null ? hendelseType.hashCode() : 0);
        result = 31 * result + (endringstype != null ? endringstype.hashCode() : 0);
        result = 31 * result + (hendelseOpprettetTid != null ? hendelseOpprettetTid.hashCode() : 0);
        result = 31 * result + (aktørId != null ? aktørId.hashCode() : 0);
        result = 31 * result + (Boolean.valueOf(erFalsk).hashCode());
        return result;
    }

    public static class Builder {
        private final PdlFalskIdentHendelsePayload kladd = new PdlFalskIdentHendelsePayload();

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

        public Builder erFalsk(boolean erFalsk) {
            this.kladd.erFalsk = erFalsk;
            return this;
        }

        public PdlFalskIdentHendelsePayload build() {
            return kladd;
        }
    }
}
