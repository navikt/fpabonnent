package no.nav.foreldrepenger.abonnent.felles.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;

public abstract class HendelsePayload {

    protected String hendelseId;

    protected String tidligereHendelseId;

    protected String hendelseType;

    protected String endringstype;

    protected LocalDateTime hendelseOpprettetTid;

    protected HendelsePayload() {
    }

    public String getHendelseId() {
        return hendelseId;
    }

    public String getTidligereHendelseId() {
        return tidligereHendelseId;
    }

    public String getHendelseType() {
        return hendelseType;
    }

    public String getEndringstype() {
        return endringstype;
    }

    public LocalDateTime getHendelseOpprettetTid() {
        return hendelseOpprettetTid;
    }

    public abstract Set<String> getAktørIderForSortering();

    public abstract HendelseWrapperDto mapPayloadTilDto();

    public abstract HendelseKilde getHendelseKilde();

    public abstract Optional<LocalDate> getHendelseDato();
}
