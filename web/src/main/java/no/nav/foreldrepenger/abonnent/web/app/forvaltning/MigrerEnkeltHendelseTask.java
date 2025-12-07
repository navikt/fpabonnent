package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "migrer.hendelse.single", maxFailedRuns = 1)
class MigrerEnkeltHendelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrerEnkeltHendelseTask.class);

    static final String HENDELSE_ID = "hendelseId";
    private final HendelseRepository hendelseRepository;
    private final MigreringKlient klient;


    @Inject
    public MigrerEnkeltHendelseTask(HendelseRepository hendelseRepository, MigreringKlient klient) {
        this.hendelseRepository = hendelseRepository;
        this.klient = klient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var hendelse = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_ID))
            .map(Long::valueOf)
            .map(hendelseRepository::finnEksaktHendelse)
            .orElseThrow();
        var dto = MigreringMapper.tilHåndtertHendelseDto(hendelse);
        LOG.info("Migrerer hendelse id {} av type {}", hendelse.getHendelseId(), hendelse.getHendelseType().name());
        klient.sendHendelse(dto);
    }

}
