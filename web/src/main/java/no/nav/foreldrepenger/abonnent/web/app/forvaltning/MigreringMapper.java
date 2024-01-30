package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class MigreringMapper {

    public static MigreringHendelseDto.HendelseDto tilHendelseDto(InngåendeHendelse hendelse) {
        return new MigreringHendelseDto.HendelseDto(hendelse.getHendelseType(), hendelse.getPayload(), hendelse.getHåndteresEtterTidspunkt(),
            hendelse.getHåndtertStatus(), hendelse.getSendtTidspunkt(), hendelse.getHendelseId(), hendelse.getTidligereHendelseId());
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

    public static MigreringProsesstaskDto.TaskDto tilProsesstaskDto(ProsessTaskData task) {
        return new MigreringProsesstaskDto.TaskDto(task.getProperties(), task.getNesteKjøringEtter());
    }

    public static ProsessTaskData fraProsesstaskDto(MigreringProsesstaskDto.TaskDto task) {
        var taskData = ProsessTaskData.forProsessTask(VurderSorteringTask.class);
        taskData.setProperties(task.taskParametere());
        taskData.setNesteKjøringEtter(task.nesteKjøringEtter());
        return taskData;
    }


}
