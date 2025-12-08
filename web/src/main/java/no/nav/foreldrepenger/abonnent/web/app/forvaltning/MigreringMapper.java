package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;

public class MigreringMapper {

    public static MigreringHendelseDto.HendelseDto tilHendelseDto(InngåendeHendelse hendelse) {
        return new MigreringHendelseDto.HendelseDto(hendelse.getHendelseType(), hendelse.getPayload(),
            hendelse.getHåndteresEtterTidspunkt(), hendelse.getHåndtertStatus(), hendelse.getSendtTidspunkt(),
            hendelse.getHendelseId(), hendelse.getTidligereHendelseId(), hendelse.getOpprettetTidspunkt());
    }

    public static MigreringHendelseDto.HendelseDto tilHåndtertHendelseDto(InngåendeHendelse hendelse) {
        return new MigreringHendelseDto.HendelseDto(hendelse.getHendelseType(), hendelse.getPayload(),
            hendelse.getHåndteresEtterTidspunkt(), HåndtertStatusType.HÅNDTERT, hendelse.getSendtTidspunkt(),
            hendelse.getHendelseId(), hendelse.getTidligereHendelseId(), hendelse.getOpprettetTidspunkt());
    }

    public static InngåendeHendelse fraHendelseDto(MigreringHendelseDto.HendelseDto hendelse) {
        return InngåendeHendelse.builder()
            .hendelseKilde(HendelseKilde.PDL)
            .hendelseType(hendelse.type())
            .payload(hendelse.payload())
            .håndteresEtterTidspunkt(hendelse.haandteresEtter())
            .håndtertStatus(hendelse.haandtertStatus())
            .sendtTidspunkt(hendelse.sendtTid())
            .hendelseId(hendelse.hendelseId())
            .tidligereHendelseId(hendelse.tidligereHendelseId())
            .build();
    }


}
