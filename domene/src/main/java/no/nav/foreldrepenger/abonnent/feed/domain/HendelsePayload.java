package no.nav.foreldrepenger.abonnent.feed.domain;

import java.util.Set;

import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;

public abstract class HendelsePayload {

    protected Long sekvensnummer;

    protected String type;

    public HendelsePayload() {
    }

    public abstract Set<String> getAktørIderForSortering();

    public abstract Long getSekvensnummer();

    public abstract String getType();

    public abstract HendelseWrapperDto mapPayloadTilDto();

    public abstract boolean erAtomisk();

    public abstract FeedKode getFeedKode();
}