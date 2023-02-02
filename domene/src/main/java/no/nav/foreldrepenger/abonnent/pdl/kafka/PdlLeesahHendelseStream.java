package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;

@ApplicationScoped
public class PdlLeesahHendelseStream implements AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseStream.class);

    private static final String APPLICATION_ID = "fpabonnent";  // Hold konstant pga offset commit !!

    private Topic<String, Personhendelse> topic;
    private KafkaStreams stream;

    PdlLeesahHendelseStream() {
    }

    @Inject
    public PdlLeesahHendelseStream(@KonfigVerdi(value = "kafka.pdl.leesah.topic") String topicName,
                                   PdlLeesahHendelseHåndterer håndterer) {
        this.topic = Topic.createConfiguredTopic(topicName);
        this.stream = createKafkaStreams(topic, håndterer);
    }

    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(Topic<String, Personhendelse> topic,
                                                   PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer) {
        final Consumed<String, Personhendelse> consumed = Consumed
                .<String, Personhendelse>with(Topology.AutoOffsetReset.LATEST)
                .withKeySerde(topic.serdeKey())
                .withValueSerde(topic.serdeValue());

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.topic(), consumed)
                .foreach(pdlLeesahHendelseHåndterer::handleMessage);

        return new KafkaStreams(builder.build(), KafkaProperties.forStreamsGenericValue(APPLICATION_ID, topic.serdeValue()));
    }

    @Override
    public boolean isAlive() {
        return (stream != null) && stream.state().isRunningOrRebalancing();
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
        stream.setUncaughtExceptionHandler((t, e) -> {
            LOG.error("{} :: Caught exception in stream, exiting", getTopicName(), e);
            stop();
        });
    }
}
