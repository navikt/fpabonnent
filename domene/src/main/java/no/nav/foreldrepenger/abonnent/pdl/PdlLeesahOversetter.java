package no.nav.foreldrepenger.abonnent.pdl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlPersonhendelse;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.foedsel.Foedsel;

@ApplicationScoped
public class PdlLeesahOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahOversetter.class);

    public static final String FØDSEL = "FOEDSEL_V1";
    public static final String DØD = "DOEDSFALL_V1";
    public static final String DØDFØDSEL = "DOEDFOEDT_BARN_V1";
    public static final String FAMILIERELASJON = "FAMILIERELASJON_V1";

    public PdlLeesahOversetter() {
        // CDI
    }

    public PdlFødsel oversettFødsel(Personhendelse personhendelse) {
        PdlFødsel.Builder builder = PdlFødsel.builder();
        oversettPersonhendelse(personhendelse, builder);
        Foedsel foedsel = personhendelse.getFoedsel();

        if (foedsel.getFoedselsaar() != null) {
            builder.medFødselsår(foedsel.getFoedselsaar());
        }
        if (foedsel.getFoedselsdato() != null) {
            builder.medFødselsdato(foedsel.getFoedselsdato());
        }
        if (foedsel.getFoedeland() != null) {
            builder.medFødeland(foedsel.getFoedeland().toString());
        }
        if (foedsel.getFoedested() != null) {
            builder.medFødested(foedsel.getFoedested().toString());
        }
        if (foedsel.getFoedekommune() != null) {
            builder.medFødekommune(foedsel.getFoedekommune().toString());
        }

        return builder.build();
    }

    public PdlDød oversettDød(Personhendelse personhendelse) {
        PdlDød.Builder builder = PdlDød.builder();
        oversettPersonhendelse(personhendelse, builder);
        builder.medDødsdato(personhendelse.getDoedsfall().getDoedsdato());
        return builder.build();
    }

    public PdlDødfødsel oversettDødfødsel(Personhendelse personhendelse) {
        PdlDødfødsel.Builder builder = PdlDødfødsel.builder();
        oversettPersonhendelse(personhendelse, builder);
        builder.medDødfødselsdato(personhendelse.getDoedfoedtBarn().getDato());
        return builder.build();
    }

    private void oversettPersonhendelse(Personhendelse personhendelse, PdlPersonhendelse.PdlPersonhendelseBuilder builder) {
        builder.medHendelseId(personhendelse.getHendelseId().toString());
        for (CharSequence ident : personhendelse.getPersonidenter()) {
            builder.leggTilPersonident(ident.toString());
        }
        if (personhendelse.getMaster() != null) {
            builder.medMaster(personhendelse.getMaster().toString());
        }
        if (personhendelse.getOpprettet() != null) {
            builder.medOpprettet(LocalDateTime.ofInstant(personhendelse.getOpprettet(), ZoneOffset.systemDefault()));
        }
        if (personhendelse.getOpplysningstype() != null) {
            builder.medOpplysningstype(personhendelse.getOpplysningstype().toString());
        }
        if (personhendelse.getEndringstype() != null) {
            builder.medEndringstype(PdlEndringstype.valueOf(personhendelse.getEndringstype().name()));
        }
        if (personhendelse.getTidligereHendelseId() != null) {
            builder.medTidligereHendelseId(personhendelse.getTidligereHendelseId().toString());
        }
        builder.medHendelseType(oversettHendelseType(personhendelse));
    }

    private HendelseType oversettHendelseType(Personhendelse personhendelse) {
        if (personhendelse.getOpplysningstype() != null && personhendelse.getEndringstype() != null) {
            String opplysningstype = personhendelse.getOpplysningstype().toString();
            Endringstype endringstype = personhendelse.getEndringstype();

            if (FØDSEL.equals(opplysningstype)) {
                if (Endringstype.OPPRETTET.equals(endringstype)) {
                    return HendelseType.PDL_FØDSEL_OPPRETTET;
                } else if (Endringstype.ANNULLERT.equals(endringstype)) {
                    return HendelseType.PDL_FØDSEL_ANNULLERT;
                } else if (Endringstype.KORRIGERT.equals(endringstype)) {
                    return HendelseType.PDL_FØDSEL_KORRIGERT;
                } else if (Endringstype.OPPHOERT.equals(endringstype)) {
                    return HendelseType.PDL_FØDSEL_OPPHØRT;
                }
            } else if (DØD.equals(opplysningstype)) {
                if (Endringstype.OPPRETTET.equals(endringstype)) {
                    return HendelseType.PDL_DØD_OPPRETTET;
                } else if (Endringstype.ANNULLERT.equals(endringstype)) {
                    return HendelseType.PDL_DØD_ANNULLERT;
                } else if (Endringstype.KORRIGERT.equals(endringstype)) {
                    return HendelseType.PDL_DØD_KORRIGERT;
                } else if (Endringstype.OPPHOERT.equals(endringstype)) {
                    return HendelseType.PDL_DØD_OPPHØRT;
                }
            } else if (DØDFØDSEL.equals(opplysningstype)) {
                if (Endringstype.OPPRETTET.equals(endringstype)) {
                    return HendelseType.PDL_DØDFØDSEL_OPPRETTET;
                } else if (Endringstype.ANNULLERT.equals(endringstype)) {
                    return HendelseType.PDL_DØDFØDSEL_ANNULLERT;
                } else if (Endringstype.KORRIGERT.equals(endringstype)) {
                    return HendelseType.PDL_DØDFØDSEL_KORRIGERT;
                } else if (Endringstype.OPPHOERT.equals(endringstype)) {
                    return HendelseType.PDL_DØDFØDSEL_OPPHØRT;
                }
            }
        }

        LOG.info("Mottok ukjent hendelsestype opplysningstype={} med endringstype={} på hendelseId={}", personhendelse.getOpplysningstype(), personhendelse.getEndringstype(), personhendelse.getHendelseId());
        return HendelseType.UDEFINERT;
    }
}
