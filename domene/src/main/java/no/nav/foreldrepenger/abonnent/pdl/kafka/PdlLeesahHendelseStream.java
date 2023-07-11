package no.nav.foreldrepenger.abonnent.pdl.kafka;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.log.metrics.LiveAndReadinessAware;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import no.nav.vedtak.log.metrics.Controllable;

@ApplicationScoped
public class PdlLeesahHendelseStream implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseStream.class);

    private static final String APPLICATION_ID = "fpabonnent";  // Hold konstant pga offset commit !!

    private Topic<String, Personhendelse> topic;
    private KafkaStreams stream;

    PdlLeesahHendelseStream() {
    }

    @Inject
    public PdlLeesahHendelseStream(@KonfigVerdi(value = "kafka.pdl.leesah.topic") String topicName, PdlLeesahHendelseHåndterer håndterer) {
        this.topic = Topic.createConfiguredTopic(topicName);
        this.stream = createKafkaStreams(topic, håndterer);
    }

    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(Topic<String, Personhendelse> topic, PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer) {
        final var consumed = Consumed.<String, Personhendelse>with(Topology.AutoOffsetReset.LATEST)
            .withKeySerde(topic.serdeKey())
            .withValueSerde(topic.serdeValue());

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.topic(), consumed).foreach(pdlLeesahHendelseHåndterer::handleMessage);

        return new KafkaStreams(builder.build(), KafkaProperties.forStreamsGenericValue(APPLICATION_ID, topic.serdeValue()));
    }

    @Override
    public boolean isAlive() {
        return (stream != null) && stream.state().isRunningOrRebalancing();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void start() {
        addShutdownHooks();
        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    @Override
    public void stop() {
        if (stream != null) {
            LOG.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
            stream.close(Duration.ofSeconds(10));
            LOG.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
        }
    }

    private String getTopicName() {
        return topic.topic();
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                LOG.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler(ex -> {
            LOG.error("{} :: Caught exception in stream, exiting", getTopicName(), ex);
            return SHUTDOWN_CLIENT;
        });
    }
}
