package no.nav.foreldrepenger.abonnent.felles.domene;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity(name = "InngåendeHendelse")
@Table(name = "INNGAAENDE_HENDELSE")
public class InngåendeHendelse extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNGAAENDE_HENDELSE")
    @Column(name = "id")
    private Long id;
    
    @Column(name = "hendelse_id")
    private String hendelseId;

    @Column(name = "tidligere_hendelse_id")
    private String tidligereHendelseId;

    @Convert(converter = HendelseKilde.KodeverdiConverter.class)
    @Column(name="kilde", nullable = false)
    private HendelseKilde hendelseKilde;

    @Convert(converter = HendelseType.KodeverdiConverter.class)
    @Column(name="type", nullable = false)
    private HendelseType hendelseType;

    @Lob
    @Column(name = "payload")
    private String payload;
    
    @Column(name = "haandteres_etter")
    private LocalDateTime håndteresEtterTidspunkt;

    @Convert(converter = HåndtertStatusType.KodeverdiConverter.class)
    @Column(name="haandtert_status", nullable = false)
    private HåndtertStatusType håndtertStatus = HåndtertStatusType.MOTTATT;

    @Column(name = "sendt_tid")
    private LocalDateTime sendtTidspunkt;

    InngåendeHendelse() {
        // Hibernate
    }

    private InngåendeHendelse(Builder builder) {
        this.id = builder.id;
        this.hendelseId = builder.hendelseId;
        this.tidligereHendelseId = builder.tidligereHendelseId;
        this.hendelseKilde = builder.hendelseKilde;
        this.hendelseType = builder.hendelseType;
        this.payload = builder.payload;
        this.håndteresEtterTidspunkt = builder.håndteresEtterTidspunkt;
        this.håndtertStatus = builder.håndtertStatus;
        this.sendtTidspunkt = builder.sendtTidspunkt;
    }

    public Long getId() {
        return id;
    }
    
    public String getHendelseId() {
        return hendelseId;
    }

    public String getTidligereHendelseId() {
        return tidligereHendelseId;
    }

    public HendelseKilde getHendelseKilde() {
        return hendelseKilde;
    }

    public HendelseType getHendelseType() {
        return hendelseType;
    }

    public String getPayload() {
        return payload;
    }   

    public LocalDateTime getHåndteresEtterTidspunkt() {
        return håndteresEtterTidspunkt;
    }

    public void setHåndteresEtterTidspunkt(LocalDateTime håndteresEtterTidspunkt) {
        this.håndteresEtterTidspunkt = håndteresEtterTidspunkt;
    }

    public HåndtertStatusType getHåndtertStatus() {
        return håndtertStatus;
    }

    public void setHåndtertStatus(HåndtertStatusType håndtertStatus) {
        this.håndtertStatus = håndtertStatus;
    }

    public void setSendtTidspunkt(LocalDateTime sendtTidspunkt) {
        this.sendtTidspunkt = sendtTidspunkt;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean erSendtTilFpsak() {
        return HåndtertStatusType.HÅNDTERT.equals(håndtertStatus) && sendtTidspunkt != null;
    }

    public boolean erFerdigbehandletMenIkkeSendtTilFpsak() {
        return HåndtertStatusType.HÅNDTERT.equals(håndtertStatus) && sendtTidspunkt == null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String hendelseId;
        private String tidligereHendelseId;
        private HendelseKilde hendelseKilde;
        private HendelseType hendelseType;
        private String payload;
        private LocalDateTime håndteresEtterTidspunkt;
        private HåndtertStatusType håndtertStatus;
        private LocalDateTime sendtTidspunkt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder tidligereHendelseId(String tidligereHendelseId) {
            this.tidligereHendelseId = tidligereHendelseId;
            return this;
        }

        public Builder hendelseKilde(HendelseKilde hendelseKilde) {
            this.hendelseKilde = hendelseKilde;
            return this;
        }

        public Builder hendelseType(HendelseType hendelseType) {
            this.hendelseType = hendelseType;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
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
