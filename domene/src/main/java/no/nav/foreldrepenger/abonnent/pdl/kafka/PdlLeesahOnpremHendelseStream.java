package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.time.Duration;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.apptjeneste.AppServiceHandler;

@ApplicationScoped
public class PdlLeesahOnpremHendelseStream implements AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahOnpremHendelseStream.class);

    private KafkaStreams stream;
    private Topic<String, Personhendelse> topic;

    PdlLeesahOnpremHendelseStream() {
    }

    @Inject
    public PdlLeesahOnpremHendelseStream(PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer,
                                         PdlLeesahOnpremHendelseProperties pdlLeesahOnpremHendelseProperties) {
        this.topic = pdlLeesahOnpremHendelseProperties.getTopic();
        LOG.info("Starter konsumering av PDL Kafka topic");
        this.stream = createKafkaStreams(topic, pdlLeesahHendelseHåndterer, pdlLeesahOnpremHendelseProperties);
    }

    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(Topic<String, Personhendelse> topic,
                                                   PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer,
                                                   PdlLeesahOnpremHendelseProperties properties) {
        if (properties.getSchemaRegistryUrl() != null && !properties.getSchemaRegistryUrl().isEmpty()) {
            var schemaMap = Map.of("schema.registry.url", properties.getSchemaRegistryUrl(), "specific.avro.reader", true);
            topic.getSerdeKey().configure(schemaMap, true);
            topic.getSerdeValue().configure(schemaMap, false);
        }

        Consumed<String, Personhendelse> consumed = Consumed.<String, Personhendelse>with(Topology.AutoOffsetReset.LATEST)
                .withKeySerde(topic.getSerdeKey())
                .withValueSerde(topic.getSerdeValue());

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.getTopic(), consumed)
                .foreach(pdlLeesahHendelseHåndterer::handleMessage);

        Topology topology = builder.build();
        return new KafkaStreams(topology, properties.getProperties());
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                LOG.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler((t, e) -> {
            LOG.error(getTopicName() + " :: Caught exception in stream, exiting", e);
            stop();
        });
    }

    @Override
    public void start() {
        addShutdownHooks();
        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    public String getTopicName() {
        return topic.getTopic();
    }

    @Override
    public void stop() {
        if (stream != null) {
            LOG.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
            stream.close(Duration.ofSeconds(10));
            LOG.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", getTopicName(), stream.state());
        }
    }

    @Override
    public boolean isAlive() {
        return stream != null && stream.state().isRunningOrRebalancing();
    }
}
