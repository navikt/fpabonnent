package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "migrer.hendelse.slett", maxFailedRuns = 1)
class SlettEnkeltHendelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SlettEnkeltHendelseTask.class);

    private final HendelseRepository hendelseRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;


    @Inject
    public SlettEnkeltHendelseTask(HendelseRepository hendelseRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(HendelserDataWrapper.HENDELSE_ID))
            .map(String::valueOf)
            .orElseThrow();
        LOG.info("Sletter hendelse_id {}", hendelse);
        hendelseRepository.slettEnkeltHendelse(hendelse);
        prosessTaskTjeneste.finnAlle(ProsessTaskStatus.KLAR).stream()
            .filter(t -> Optional.ofNullable(t.getPropertyValue(HendelserDataWrapper.HENDELSE_ID)).filter(h -> h.equals(hendelse)).isPresent())
            .forEach(t -> prosessTaskTjeneste.setProsessTaskFerdig(t.getId(), ProsessTaskStatus.KLAR));
    }

}
