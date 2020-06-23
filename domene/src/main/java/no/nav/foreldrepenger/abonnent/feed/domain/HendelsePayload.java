package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDateTime;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;

public abstract class HendelsePayload {

    protected String hendelseId;

    protected String tidligereHendelseId;

    protected String type;

    protected String endringstype;

    protected LocalDateTime hendelseOpprettetTid;

    public HendelsePayload() {
    }

    public String getHendelseId() {
        return hendelseId;
    }

    public String getTidligereHendelseId() {
        return tidligereHendelseId;
    }

    public String getType() {
        return type;
    }

    public String getEndringstype() {
        return endringstype;
    }

    public LocalDateTime getHendelseOpprettetTid() {
        return hendelseOpprettetTid;
    }

    public abstract Set<String> getAktørIderForSortering();

    public abstract HendelseWrapperDto mapPayloadTilDto();

    public abstract boolean erAtomisk();

    public abstract FeedKode getFeedKode();
}