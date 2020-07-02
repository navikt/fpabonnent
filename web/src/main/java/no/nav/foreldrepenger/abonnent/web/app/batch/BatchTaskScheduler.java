package no.nav.foreldrepenger.abonnent.web.app.batch;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTypeInfo;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskType;
import no.nav.vedtak.felles.prosesstask.impl.cron.CronExpression;

/**
 * Starter tasks med cron-expression hvis disse ikke har noen status fra før av.
 */
@ApplicationScoped
@ActivateRequestContext
@Transactional
//TODO(JEJ): Erstatte med samme klasse i nyere versjon av fp-prosesstask etter oppgradering av felles-biblioteker
public class BatchTaskScheduler implements AppServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(BatchTaskScheduler.class);
    private ProsessTaskRepository taskRepository;

    BatchTaskScheduler() {
    }

    @Inject
    public BatchTaskScheduler(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void start() {
        Map<ProsessTaskType, ProsessTaskEntitet> statusForBatchTasks = taskRepository.finnStatusForBatchTasks();
        statusForBatchTasks.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> ProsessTaskStatus.FEILET.equals(entry.getValue().getStatus()))
                .forEach(this::restartTask);

        statusForBatchTasks = taskRepository.finnStatusForBatchTasks();
        statusForBatchTasks.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == null)
                .forEach(this::opprettTaskForType);
    }

    private void restartTask(Map.Entry<ProsessTaskType, ProsessTaskEntitet> entry) {
        final var pte = entry.getValue();
        final var eksisterendeProsessTaskData = taskRepository.finn(pte.getId());
        eksisterendeProsessTaskData.setStatus(ProsessTaskStatus.KLAR);
        eksisterendeProsessTaskData.setNesteKjøringEtter(LocalDateTime.now());
        eksisterendeProsessTaskData.setSisteFeilKode(null);
        eksisterendeProsessTaskData.setSisteFeil(null);

        /**
         * Tvungen kjøring: reduserer anall feilede kjøring med 1 slik at {@link no.nav.k9.sak.felles.prosesstask.impl.TaskManager}
         * kan plukke den opp og kjøre.
         */
        Optional<ProsessTaskTypeInfo> taskTypeInfo = taskRepository.finnProsessTaskType(eksisterendeProsessTaskData.getTaskType());
        if (taskTypeInfo.get().getMaksForsøk() == eksisterendeProsessTaskData.getAntallFeiledeForsøk()) { // NOSONAR
            eksisterendeProsessTaskData.setAntallFeiledeForsøk(eksisterendeProsessTaskData.getAntallFeiledeForsøk() - 1);
        }
        log.info("Restarter batch-task da siste='{}' står til feilet.", entry.getValue());
        taskRepository.lagre(eksisterendeProsessTaskData);
    }

    private void opprettTaskForType(Map.Entry<ProsessTaskType, ProsessTaskEntitet> entry) {
        ProsessTaskType type = entry.getKey();
        ProsessTaskData data = new ProsessTaskData(type.getKode());
        final String cron = type.getCronExpression();
        LocalDateTime neste = new CronExpression(cron).neste(LocalDateTime.now());
        data.setNesteKjøringEtter(neste);
        if (entry.getValue() != null) {
            log.info("Oppretter ny='{}' batch-task da siste='{}' har ikke er klar for kjøring.", data, entry.getValue().tilProsessTask());
        }
        taskRepository.lagre(data);
    }

    @Override
    public void stop() {

    }
}