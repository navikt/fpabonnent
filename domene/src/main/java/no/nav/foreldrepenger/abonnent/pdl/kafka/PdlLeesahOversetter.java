package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDød;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlDødfødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlPersonhendelse;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlUtflytting;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;

@ApplicationScoped
public class PdlLeesahOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahOversetter.class);

    public static final String FØDSEL = "FOEDSEL_V1";
    public static final String DØD = "DOEDSFALL_V1";
    public static final String DØDFØDSEL = "DOEDFOEDT_BARN_V1";
    public static final String UTFLYTTING = "UTFLYTTING_FRA_NORGE";

    public PdlLeesahOversetter() {
        // CDI
    }

    public PdlFødsel oversettFødsel(Personhendelse personhendelse) {
        var builder = PdlFødsel.builder();
        oversettPersonhendelse(personhendelse, builder);

        if (personhendelse.getFoedsel() != null) {
            builder.medFødselsdato(personhendelse.getFoedsel().getFoedselsdato());
        }

        return builder.build();
    }

    public PdlDød oversettDød(Personhendelse personhendelse) {
        var builder = PdlDød.builder();
        oversettPersonhendelse(personhendelse, builder);
        if (personhendelse.getDoedsfall() != null) {
            builder.medDødsdato(personhendelse.getDoedsfall().getDoedsdato());
        }
        return builder.build();
    }

    public PdlUtflytting oversettUtflytting(Personhendelse personhendelse) {
        var builder = PdlUtflytting.builder();
        oversettPersonhendelse(personhendelse, builder);
        if (personhendelse.getUtflyttingFraNorge() != null) {
            builder.medUtflyttingsdato(personhendelse.getUtflyttingFraNorge().getUtflyttingsdato());
        }
        return builder.build();
    }

    public PdlDødfødsel oversettDødfødsel(Personhendelse personhendelse) {
        var builder = PdlDødfødsel.builder();
        oversettPersonhendelse(personhendelse, builder);
        if (personhendelse.getDoedfoedtBarn() != null) {
            builder.medDødfødselsdato(personhendelse.getDoedfoedtBarn().getDato());
        }
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

            switch (opplysningstype) {
                case FØDSEL:
                    return switch (endringstype) {
                        case OPPRETTET -> HendelseType.PDL_FØDSEL_OPPRETTET;
                        case ANNULLERT -> HendelseType.PDL_FØDSEL_ANNULLERT;
                        case KORRIGERT -> HendelseType.PDL_FØDSEL_KORRIGERT;
                        case OPPHOERT -> HendelseType.PDL_FØDSEL_OPPHØRT;
                    };
                case DØD:
                    return switch (endringstype) {
                        case OPPRETTET -> HendelseType.PDL_DØD_OPPRETTET;
                        case ANNULLERT -> HendelseType.PDL_DØD_ANNULLERT;
                        case KORRIGERT -> HendelseType.PDL_DØD_KORRIGERT;
                        case OPPHOERT -> HendelseType.PDL_DØD_OPPHØRT;
                    };
                case DØDFØDSEL:
                    return switch (endringstype) {
                        case OPPRETTET -> HendelseType.PDL_DØDFØDSEL_OPPRETTET;
                        case ANNULLERT -> HendelseType.PDL_DØDFØDSEL_ANNULLERT;
                        case KORRIGERT -> HendelseType.PDL_DØDFØDSEL_KORRIGERT;
                        case OPPHOERT -> HendelseType.PDL_DØDFØDSEL_OPPHØRT;
                    };
                case UTFLYTTING:
                    return switch (endringstype) {
                        case OPPRETTET -> HendelseType.PDL_UTFLYTTING_OPPRETTET;
                        case ANNULLERT -> HendelseType.PDL_UTFLYTTING_ANNULLERT;
                        case KORRIGERT -> HendelseType.PDL_UTFLYTTING_KORRIGERT;
                        case OPPHOERT -> HendelseType.PDL_UTFLYTTING_OPPHØRT;
                    };
                default:
                    break;
            }
        }

        LOG.info("Mottok ukjent hendelsestype opplysningstype={} med endringstype={} på hendelseId={}", personhendelse.getOpplysningstype(), personhendelse.getEndringstype(), personhendelse.getHendelseId());
        return HendelseType.UDEFINERT;
    }
}
