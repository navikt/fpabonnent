package no.nav.foreldrepenger.abonnent.felles;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import no.nav.foreldrepenger.abonnent.kodeverk.Kodeliste;

@Entity(name = "FeedKode")
@DiscriminatorValue(FeedKode.DISCRIMINATOR)
public class FeedKode extends Kodeliste {

    public static final String DISCRIMINATOR = "FEED_KODE";
    public static final FeedKode UDEFINERT = new FeedKode("-"); //$NON-NLS-1$
    public static final FeedKode TPS = new FeedKode("JF_TPS"); //$NON-NLS-1$
    public static final FeedKode INFOTRYGD = new FeedKode("JF_INFOTRYGD"); //$NON-NLS-1$

    @SuppressWarnings("unused")
    private FeedKode() {
        // Hibernate
    }

    public FeedKode(String kode) {
        super(kode, DISCRIMINATOR);
    }
}