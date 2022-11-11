package no.nav.foreldrepenger.abonnent.pdl.kafka;

import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.apptjeneste.AppServiceHandler;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AivenStream implements AppServiceHandler, KafkaIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(AivenStream.class);


    private Topic<String, Personhendelse> topic;
    private KafkaStreams stream;

    AivenStream() {
    }

    @Inject
    public AivenStream(PdlLeesahHendelseHåndterer håndterer, AivenProperties streamKafkaProperties) {
        this.topic = streamKafkaProperties.getTopic();
        this.stream = createKafkaStreams(topic, håndterer, streamKafkaProperties);
    }

    @SuppressWarnings("resource")
    private static KafkaStreams createKafkaStreams(Topic<String, Personhendelse> topic,
                                                   PdlLeesahHendelseHåndterer pdlLeesahHendelseHåndterer, // ubrukt inntil offset satt
                                                   AivenProperties properties) {
        final Consumed<String, Personhendelse> consumed = Consumed
                .<String, Personhendelse>with(Topology.AutoOffsetReset.EARLIEST)
                .withKeySerde(topic.getSerdeKey())
                .withValueSerde(topic.getSerdeValue());

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.getTopic(), consumed)
                .foreach((k, v) -> LOG.info("PDL Aivenstream leser melding med hendelseId {}", v.getHendelseId()));
                //.foreach(pdlLeesahHendelseHåndterer::handleMessage);

        return new KafkaStreams(builder.build(), properties.getProperties());
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

    }

    private String getTopicName() {
        return topic.getTopic();
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
