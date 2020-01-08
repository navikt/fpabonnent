package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.abonnent.felles.BaseEntitet;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.vedtak.util.FPDateUtil;

@Entity(name = "InputFeed")
@Table(name = "INPUT_FEED")
public class InputFeed extends BaseEntitet {

    @Id
    @Column(name = "kode")
    private String kode; //Burde vært FeedKode, men det er ikke mulig sammen med "for update skip locked" pga HHH-7525

    @Column(name = "navn")
    private String navn;

    @Column(name = "sist_lest")
    private LocalDateTime sistLest;

    @Column(name = "sist_feilet")
    private LocalDateTime sistFeilet;

    @Column(name = "feilet_antall")
    private Integer feiletAntall;

    @Column(name = "next_url")
    private String nextUrl;

    @Column(name = "ventetid_ferdiglest")
    private String ventetidFerdiglest;

    @Column(name = "ventetid_lesbar")
    private String ventetidLesbar;

    @Column(name = "ventetid_feilet")
    private String ventetidFeilet;

    public InputFeed() {
        //hibernate
    }

    public void oppdaterLestOk(String nextUrl) {
        sistLest = LocalDateTime.now(FPDateUtil.getOffset());
        feiletAntall = 0;
        this.nextUrl = nextUrl;
    }

    public void oppdaterFeilet() {
        sistFeilet = LocalDateTime.now(FPDateUtil.getOffset());
        feiletAntall++;
    }

    public FeedKode getKode() {
        return FeedKode.fraKode(kode);
    }

    public Optional<LocalDateTime> getSistLest() {
        return Optional.ofNullable(sistLest);
    }

    public Optional<LocalDateTime> getSistFeilet() {
        return Optional.ofNullable(sistFeilet);
    }

    public Optional<String> getNextUrl() {
        return Optional.ofNullable(nextUrl);
    }

    public Duration getVentetidFerdiglest() {
        return Duration.parse(ventetidFerdiglest);
    }

    public Duration getVentetidLesbar() {
        return Duration.parse(ventetidLesbar);
    }

    public Duration getVentetidFeilet() {
        return Duration.parse(ventetidFeilet);
    }

    public Integer getFeiletAntall() {
        return feiletAntall;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String kode;
        private String navn;
        private LocalDateTime sistLest;
        private LocalDateTime sistFeilet;
        private Integer feiletAntall;
        private String nextUrl;
        private String ventetidFerdiglest;
        private String ventetidLesbar;
        private String ventetidFeilet;

        public Builder kode(FeedKode feedKode) {
            this.kode = feedKode.getKode();
            return this;
        }

        public Builder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder sistLest(LocalDateTime sistLest) {
            this.sistLest = sistLest;
            return this;
        }

        public Builder sistFeilet(LocalDateTime sistFeilet) {
            this.sistFeilet = sistFeilet;
            return this;
        }

        public Builder feiletAntall(Integer feiletAntall) {
            this.feiletAntall = feiletAntall;
            return this;
        }

        public Builder nextUrl(String nextUrl) {
            this.nextUrl = nextUrl;
            return this;
        }

        public Builder ventetidFerdiglest(String ventetidFerdiglest) {
            this.ventetidFerdiglest = ventetidFerdiglest;
            return this;
        }

        public Builder ventetidLesbar(String ventetidLesbar) {
            this.ventetidLesbar = ventetidLesbar;
            return this;
        }

        public Builder ventetidFeilet(String ventetidFeilet) {
            this.ventetidFeilet = ventetidFeilet;
            return this;
        }

        public InputFeed build() {
            InputFeed inputFeed = new InputFeed();
            inputFeed.kode = kode;
            inputFeed.navn = navn;
            inputFeed.sistLest = sistLest;
            inputFeed.sistFeilet = sistFeilet;
            inputFeed.feiletAntall = feiletAntall;
            inputFeed.nextUrl = nextUrl;
            inputFeed.ventetidFerdiglest = ventetidFerdiglest;
            inputFeed.ventetidLesbar = ventetidLesbar;
            inputFeed.ventetidFeilet = ventetidFeilet;
            return inputFeed;
        }
    }

    @Override
    public String toString() {
        return "InputFeed [kode=" + kode + ", navn=" + navn + ", sistLest=" + sistLest + ", sistFeilet=" + sistFeilet + ", feiletAntall="
                + feiletAntall + ", nextUrl=" + nextUrl + ", ventetidFerdiglest=" + ventetidFerdiglest + ", ventetidLesbar="
                + ventetidLesbar + ", ventetidFeilet=" + ventetidFeilet + "]";
    }
}
