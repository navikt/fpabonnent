package no.nav.foreldrepenger.abonnent.pdl.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaConsumerManager;
import no.nav.vedtak.server.Controllable;
import no.nav.vedtak.server.LiveAndReadinessAware;

@ApplicationScoped
public class PdlLeesahHendelseConsumer implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseConsumer.class);

    private KafkaConsumerManager<String, Personhendelse> kcm;

    PdlLeesahHendelseConsumer() {
    }

    @Inject
    public PdlLeesahHendelseConsumer(
        PdlLeesahHendelseHåndterer håndterer) {
        this.kcm = new KafkaConsumerManager<>(håndterer);
    }

    @Override
    public boolean isAlive() {
        return kcm.allRunning();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void start() {
        LOG.info("Starter konsumering av topics={}", kcm.topicNames());
        kcm.start((t, e) -> LOG.error("{} :: Caught exception in stream, exiting", t, e));
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topics={} med 10 sekunder timeout", kcm.topicNames());
        kcm.stop();
    }

}
