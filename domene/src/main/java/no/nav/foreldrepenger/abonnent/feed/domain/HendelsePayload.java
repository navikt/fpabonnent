package no.nav.foreldrepenger.abonnent.feed.domain;

import java.util.Set;

import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;

public abstract class HendelsePayload {

    protected String hendelseId;

    protected String type;

    protected String endringstype;

    public HendelsePayload() {
    }

    public String getHendelseId() {
        return hendelseId;
    }

    public String getType() {
        return type;
    }

    public String getEndringstype() {
        return endringstype;
    }

    public abstract Set<String> getAktørIderForSortering();

    public abstract HendelseWrapperDto mapPayloadTilDto();

    public abstract boolean erAtomisk();

    public abstract FeedKode getFeedKode();
}