package no.nav.foreldrepenger.abonnent.feed.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import no.nav.foreldrepenger.abonnent.kodeverk.Kodeliste;

@Entity(name = "HåndtertStatusType")
@DiscriminatorValue(HåndtertStatusType.DISCRIMINATOR)
public class HåndtertStatusType extends Kodeliste {

    public static final String DISCRIMINATOR = "HAANDTERT_STATUS";
    public static final HåndtertStatusType MOTTATT = new HåndtertStatusType("MOTTATT"); //$NON-NLS-1$
    public static final HåndtertStatusType SENDT_TIL_SORTERING = new HåndtertStatusType("SENDT_TIL_SORTERING"); //$NON-NLS-1$
    public static final HåndtertStatusType GROVSORTERT = new HåndtertStatusType("GROVSORTERT"); //$NON-NLS-1$
    public static final HåndtertStatusType HÅNDTERT = new HåndtertStatusType("HÅNDTERT"); //$NON-NLS-1$

    HåndtertStatusType() {
        // Hibernate trenger den
    }

    private HåndtertStatusType(String kode) {
        super(kode, DISCRIMINATOR);
    }
}
