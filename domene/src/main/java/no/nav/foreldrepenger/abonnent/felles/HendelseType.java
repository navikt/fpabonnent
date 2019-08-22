package no.nav.foreldrepenger.abonnent.felles;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import no.nav.foreldrepenger.abonnent.kodeverk.Kodeliste;

@Entity(name = "HendelseType")
@DiscriminatorValue(HendelseType.DISCRIMINATOR)
public class HendelseType extends Kodeliste {

    public static final String DISCRIMINATOR = "HENDELSE_TYPE";
    public static final HendelseType UDEFINERT = new HendelseType("-"); //$NON-NLS-1$
    public static final HendelseType FØDSELSMELDINGOPPRETTET = new HendelseType("FOEDSELSMELDINGOPPRETTET"); //$NON-NLS-1$
    public static final HendelseType DØDSMELDINGOPPRETTET = new HendelseType("DOEDSMELDINGOPPRETTET"); //$NON-NLS-1$
    public static final HendelseType DØDFØDSELOPPRETTET = new HendelseType("DOEDFOEDSELOPPRETTET"); //$NON-NLS-1$
    public static final HendelseType OPPHØERT = new HendelseType("OPPHOERT_v1"); //$NON-NLS-1$
    public static final HendelseType INNVILGET = new HendelseType("INNVILGET_v1"); //$NON-NLS-1$
    public static final HendelseType ANNULLERT = new HendelseType("ANNULLERT_v1"); //$NON-NLS-1$
    public static final HendelseType ENDRET = new HendelseType("ENDRET_v1"); //$NON-NLS-1$

    @SuppressWarnings("unused")
    private HendelseType() {
        // Hibernate
    }

    public HendelseType(String kode) {
        super(kode, DISCRIMINATOR);
    }

    public static boolean erYtelseHendelseType(HendelseType hendelseType) {
        return OPPHØERT.equals(hendelseType)
                || INNVILGET.equals(hendelseType)
                || ANNULLERT.equals(hendelseType)
                || ENDRET.equals(hendelseType);
    }
}
