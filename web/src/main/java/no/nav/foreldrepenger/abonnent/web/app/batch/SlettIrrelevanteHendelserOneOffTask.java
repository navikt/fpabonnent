package no.nav.foreldrepenger.abonnent.web.app.batch;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "slett.hendelserOneOff")
public class SlettIrrelevanteHendelserOneOffTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SlettIrrelevanteHendelserOneOffTask.class);

    private final HendelseRepository hendelseRepository;

    @Inject
    public SlettIrrelevanteHendelserOneOffTask(HendelseRepository hendelseRepository) {
        this.hendelseRepository = hendelseRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var slettet = hendelseRepository.slettGamleHendelser();
        LOG.info("Slettet {} hendelser som er foreldet.", slettet);
    }
}
