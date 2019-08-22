package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

import no.nav.foreldrepenger.abonnent.felles.FeedKode;
import no.nav.foreldrepenger.abonnent.felles.HendelseType;
import no.nav.vedtak.felles.jpa.BaseEntitet;

@Entity(name = "InngåendeHendelse")
@Table(name = "INNGAAENDE_HENDELSE")
public class InngåendeHendelse extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNGAAENDE_HENDELSE")
    @Column(name = "id")
    private Long id;
    
    @Column(name = "sekvensnummer")
    private Long sekvensnummer;

    @Column(name = "kobling_id")
    private Long koblingId;

    @ManyToOne(optional = false)
    @JoinColumnOrFormula(column = @JoinColumn(name = "feed_kode", referencedColumnName = "kode", nullable = false))
    @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "kodeverk", value = "'" + FeedKode.DISCRIMINATOR + "'"))
    private FeedKode feedKode;

    @ManyToOne(optional = false)
    @JoinColumnOrFormula(column = @JoinColumn(name = "type", referencedColumnName = "kode", nullable = false))
    @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "kodeverk", value = "'" + HendelseType.DISCRIMINATOR + "'"))
    private HendelseType type;

    @Lob
    @Column(name = "payload")
    private String payload;
    
    @Column(name = "REQUEST_UUID")
    private String requestUuid;

    @Column(name = "haandteres_etter")
    private LocalDateTime håndteresEtterTidspunkt;

    @ManyToOne(optional = false)
    @JoinColumnOrFormula(column = @JoinColumn(name = "haandtert_status", referencedColumnName = "kode", nullable = false))
    @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "kodeverk", value = "'" + HåndtertStatusType.DISCRIMINATOR + "'"))
    private HåndtertStatusType håndtertStatus = HåndtertStatusType.MOTTATT;

    @Column(name = "sendt_tid")
    private LocalDateTime sendtTidspunkt;

    InngåendeHendelse() {
        // Hibernate
    }

    private InngåendeHendelse(Builder builder) {
        this.id = builder.id;
        this.sekvensnummer = builder.sekvensnummer;
        this.koblingId = builder.koblingId;
        this.feedKode = builder.feedKode;
        this.type = builder.type;
        this.payload = builder.payload;
        this.requestUuid = builder.requestUuid;
        this.håndteresEtterTidspunkt = builder.håndteresEtterTidspunkt;
        this.håndtertStatus = builder.håndtertStatus;
        this.sendtTidspunkt = builder.sendtTidspunkt;
    }

    public Long getId() {
        return id;
    }
    
    public Long getSekvensnummer() {
        return sekvensnummer;
    }

    public Long getKoblingId() {
        return koblingId;
    }

    public FeedKode getFeedKode() {
        return feedKode;
    }

    public HendelseType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }   

    public String getRequestUuid() {
        return requestUuid;
    }

    public LocalDateTime getHåndteresEtterTidspunkt() {
        return håndteresEtterTidspunkt;
    }

    public HåndtertStatusType getHåndtertStatus() {
        return håndtertStatus;
    }

    void setHåndtertStatus(HåndtertStatusType håndtertStatus) {
        this.håndtertStatus = håndtertStatus;
    }

    void setSendtTidspunkt(LocalDateTime sendtTidspunkt) {
        this.sendtTidspunkt = sendtTidspunkt;
    }

    void setPayload(String payload) {
        this.payload = payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long sekvensnummer;
        private Long koblingId;
        private FeedKode feedKode;
        private HendelseType type;
        private String payload;
        private String requestUuid;
        private LocalDateTime håndteresEtterTidspunkt;
        private HåndtertStatusType håndtertStatus;
        private LocalDateTime sendtTidspunkt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder sekvensnummer(Long sekvensnummer) {
            this.sekvensnummer = sekvensnummer;
            return this;
        }

        public Builder koblingId(Long koblingId) {
            this.koblingId = koblingId;
            return this;
        }

        public Builder feedKode(FeedKode feedKode) {
            this.feedKode = feedKode;
            return this;
        }

        public Builder type(HendelseType type) {
            this.type = type;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder requestUuid(String requestUuid) {
            this.requestUuid = requestUuid;
            return this;
        }

        public Builder håndteresEtterTidspunkt(LocalDateTime håndteresEtterTidspunkt) {
            this.håndteresEtterTidspunkt = håndteresEtterTidspunkt;
            return this;
        }

        public Builder håndtertStatus(HåndtertStatusType håndtertStatus) {
            this.håndtertStatus = håndtertStatus;
            return this;
        }

        public Builder sendtTidspunkt(LocalDateTime sendtTidspunkt) {
            this.sendtTidspunkt = sendtTidspunkt;
            return this;
        }

        public InngåendeHendelse build() {
            return new InngåendeHendelse(this);
        }
    }
}
