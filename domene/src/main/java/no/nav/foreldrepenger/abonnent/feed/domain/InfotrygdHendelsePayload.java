package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.felles.AbonnentHendelserFeil;
import no.nav.foreldrepenger.abonnent.felles.FeedKode;
import no.nav.foreldrepenger.abonnent.fpsak.consumer.HendelseMapper;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDtoBuilder;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype;

public class InfotrygdHendelsePayload extends HendelsePayload {

    private Long koblingId;

    private String aktoerId;

    private LocalDate fom;

    private String identDato;

    private String typeYtelse;

    public InfotrygdHendelsePayload() {
    }

    private InfotrygdHendelsePayload(Builder builder) {
        this.sekvensnummer = builder.sekvensnummer;
        this.koblingId = builder.koblingId;
        this.type = builder.type;
        this.aktoerId = builder.aktørId;
        this.fom = builder.fom;
        this.identDato = builder.identDato;
        this.typeYtelse = builder.typeYtelse;
    }

    @Override
    public HendelseWrapperDto mapPayloadTilDto() {

        InfotrygdHendelseDtoBuilder builder = getBuilderBasertPåType(this.getType());

        InfotrygdHendelseDto dto = builder
                .medAktørId(this.getAktoerId())
                .medFraOgMed(this.getFom())
                .medIdentdato(this.getIdentDato())
                .medTypeYtelse(this.getTypeYtelse())
                .medUnikId(HendelseMapper.INFOTRYGD_HENDELSE_TYPE + this.getSekvensnummer())
                .build();
        return HendelseWrapperDto.lagDto(dto);
    }

    private InfotrygdHendelseDtoBuilder getBuilderBasertPåType(String typeFeedHendelse) {
        if (Meldingstype.INFOTRYGD_INNVILGET.getType().equals(typeFeedHendelse)) {
            return InfotrygdHendelseDtoBuilder.innvilget();
        } else if (Meldingstype.INFOTRYGD__OPPHOERT.getType().equals(typeFeedHendelse)) {
            return InfotrygdHendelseDtoBuilder.opphørt();
        } else if (Meldingstype.INFOTRYGD_ANNULLERT.getType().equals(typeFeedHendelse)) {
            return InfotrygdHendelseDtoBuilder.annulert();
        } else if (Meldingstype.INFOTRYGD_ENDRET.getType().equals(typeFeedHendelse)) {
            return InfotrygdHendelseDtoBuilder.endring();
        } else {
            throw AbonnentHendelserFeil.FACTORY.ukjentHendelseType(typeFeedHendelse).toException();
        }
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public String getIdentDato() {
        return identDato;
    }

    public void setIdentDato(String identDato) {
        this.identDato = identDato;
    }

    public String getTypeYtelse() {
        return typeYtelse;
    }

    public void setTypeYtelse(String typeYtelse) {
        this.typeYtelse = typeYtelse;
    }

    @Override
    public boolean erAtomisk() {
        return false;
    }

    @Override
    public FeedKode getFeedKode() {
        return FeedKode.INFOTRYGD;
    }

    @Override
    public Set<String> getAktørIderForSortering() {
        Set<String> set = new HashSet<>();
        set.add(aktoerId);
        return set;
    }

    @Override
    public Long getSekvensnummer() {
        return sekvensnummer;
    }

    public Long getKoblingId() {
        return koblingId;
    }

    @Override
    public String getType() {
        return type;
    }

    public static class Builder {
        private String aktørId;

        private LocalDate fom;

        private String identDato;

        private String typeYtelse;

        private Long sekvensnummer;

        private Long koblingId;

        private String type;

        public Builder aktørId(String value) {
            this.aktørId = value;
            return this;
        }

        public Builder fom(LocalDate value) {
            this.fom = value;
            return this;
        }

        public Builder identDato(String value) {
            this.identDato = value;
            return this;
        }

        public Builder typeYtelse(String value) {
            this.typeYtelse = value;
            return this;
        }

        public Builder sekvensnummer(Long value) {
            this.sekvensnummer = value;
            return this;
        }

        public Builder koblingId(Long value) {
            this.koblingId = value;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public InfotrygdHendelsePayload build() {
            return new InfotrygdHendelsePayload(this);
        }

    }
}
