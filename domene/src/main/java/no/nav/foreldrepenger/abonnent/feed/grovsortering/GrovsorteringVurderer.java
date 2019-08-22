package no.nav.foreldrepenger.abonnent.feed.grovsortering;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.feed.domain.HendelseRepository;
import no.nav.foreldrepenger.abonnent.feed.domain.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.task.SorterHendelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/**
 * Sjekker om det er kommet noen hendelser som er klare for grovsortering
 * og oppretter prosess tasks som gjør selve grovsorteringen.
 */
@ApplicationScoped
public class GrovsorteringVurderer {

    private static final Logger log = LoggerFactory.getLogger(GrovsorteringVurderer.class);

    private HendelseRepository hendelseRepository;
    private ProsessTaskRepository prosessTaskRepository;

    GrovsorteringVurderer() {
        // CDI
    }

    @Inject
    public GrovsorteringVurderer(HendelseRepository hendelseRepository, ProsessTaskRepository prosessTaskRepository) {
        this.hendelseRepository = hendelseRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public synchronized void vurderGrovsortering() {
        try {
            List<InngåendeHendelse> inngåendeHendelser = hendelseRepository.finnHendelserSomErKlareTilGrovsortering();
            inngåendeHendelser.forEach(ih -> hendelseRepository.oppdaterHåndtertStatus(ih, HåndtertStatusType.SENDT_TIL_SORTERING));
            finnUnikeRequestUuider(inngåendeHendelser).forEach(this::opprettSorteringTask);
        } catch (Exception e) {
            GrovsorteringFeil.FACTORY.kanIkkeSjekkeEtterHendelserTilGrovsortering(e).log(log);
        }
    }

    private Set<String> finnUnikeRequestUuider(List<InngåendeHendelse> inngåendeHendelser) {
        return inngåendeHendelser.stream().map(InngåendeHendelse::getRequestUuid)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).keySet();
    }

    private void opprettSorteringTask(String pollId) {
        HendelserDataWrapper grovsorteringTask = new HendelserDataWrapper(new ProsessTaskData(SorterHendelserTask.TASKNAME));
        grovsorteringTask.setHendelseRequestUuid(pollId);
        prosessTaskRepository.lagre(grovsorteringTask.getProsessTaskData());
    }
}
