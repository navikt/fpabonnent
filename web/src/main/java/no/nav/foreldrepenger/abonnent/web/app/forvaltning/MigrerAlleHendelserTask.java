package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "migrer.hendelse.alle", maxFailedRuns = 1)
class MigrerAlleHendelserTask implements ProsessTaskHandler {

    private static final String FRA_HENDELSE_ID = "fraHendelseId";
    private final HendelseRepository hendelseRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public MigrerAlleHendelserTask(HendelseRepository hendelseRepository,
                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraHendelseId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_HENDELSE_ID))
            .map(Long::valueOf).orElse(0L);

        var hendelser = hendelseRepository.finnNesteHundreHendelser(fraHendelseId);

        if (hendelser.isEmpty()) {
            return;
        }
        var gruppe = new ProsessTaskGruppe();
        var tasks = hendelser.stream().map(MigrerAlleHendelserTask::opprettTaskForEnkeltSak).toList();
        gruppe.addNesteParallell(tasks);
        prosessTaskTjeneste.lagre(gruppe);

        hendelser.stream().max(Comparator.naturalOrder())
            .map(MigrerAlleHendelserTask::opprettTaskForNesteUtvalg)
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long hendelseId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerEnkeltHendelseTask.class);
        prosessTaskData.setProperty(MigrerEnkeltHendelseTask.HENDELSE_ID, String.valueOf(hendelseId));
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraHendelseId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(MigrerAlleHendelserTask.class);
        prosessTaskData.setProperty(MigrerAlleHendelserTask.FRA_HENDELSE_ID, String.valueOf(fraHendelseId));
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(6));
        return prosessTaskData;
    }
}
