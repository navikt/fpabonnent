package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "HendelseLock")
@Table(name = "HENDELSE_LOCK")
public class HendelseLock {

    @Id
    private Long id;

    @Column(name = "sist_laast_tid")
    private LocalDateTime sistLåstTidspunkt;

    public void oppdaterSistLåstTidspunkt() {
        this.sistLåstTidspunkt = LocalDateTime.now();
    }
}
