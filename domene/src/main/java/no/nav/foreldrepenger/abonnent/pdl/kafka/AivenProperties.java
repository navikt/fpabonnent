package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import no.nav.foreldrepenger.abonnent.pdl.kafka.test.VtpKafkaAvroSerde;
import no.nav.foreldrepenger.konfig.Environment;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;

@Dependent
public class AivenProperties {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseProperties.class);

    private static final String KAFKA_AVRO_SERDE_CLASS = "kafka.avro.serde.class";

    private static final Environment ENV = Environment.current();

    private final String bootstrapServers;
    private final String applicationId;
    private final String trustStorePath;
    private final String keyStoreLocation;
    private final String credStorePassword;
    private final String clientId;

    private final Topic<String, Personhendelse> topic;

    private final boolean isDeployment = ENV.isProd() || ENV.isDev();

    @Inject
    public AivenProperties(@KonfigVerdi(value = "kafka.aiven.pdl.leesah.topic", defaultVerdi = "default") String topicName,
                           @KonfigVerdi(value = "kafka.pdl.leesah.topic") String onpremTopicName,
                           @KonfigVerdi("KAFKA_BROKERS") String bootstrapServers,
                           @KonfigVerdi("KAFKA_SCHEMA_REGISTRY") String schemaRegistryUrl,
                           @KonfigVerdi("KAFKA_SCHEMA_REGISTRY_USER") String schemaRegistryUsername,
                           @KonfigVerdi("KAFKA_SCHEMA_REGISTRY_PASSWORD") String schemaRegistryPassword,
                           @KonfigVerdi("KAFKA_TRUSTSTORE_PATH") String trustStorePath,
                           @KonfigVerdi("KAFKA_KEYSTORE_PATH") String keyStoreLocation,
                           @KonfigVerdi("KAFKA_CREDSTORE_PASSWORD") String credStorePassword,
                           @KonfigVerdi(value = KAFKA_AVRO_SERDE_CLASS, defaultVerdi = "io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde") String kafkaAvroSerdeClass) {
        this.trustStorePath = trustStorePath;
        this.keyStoreLocation = keyStoreLocation;
        this.credStorePassword = credStorePassword;
        this.applicationId = "fpabonnent";
        this.clientId = "fpabonnent-" + UUID.randomUUID();
        this.bootstrapServers = bootstrapServers;
        // Utleder korrekt topic for å unngå innføring av nye midlertidige verdier i VTP inntil vi er over på Aiven
        if (isDeployment && topicName.equals("default")) {
            throw new IllegalStateException("Konfigurasjonsfeil: mangler verdi for kafka.aiven.pdl.leesah.topic");
        }
        var utledetTopic = !isDeployment ? onpremTopicName : topicName;
        this.topic = createConfiguredTopic(utledetTopic, schemaRegistryUrl, getBasicAuth(schemaRegistryUsername, schemaRegistryPassword));
    }

    public Topic<String, Personhendelse> getTopic() {
        return topic;
    }

    public Properties getProperties() {
        final Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        LOG.info("Stream APPLICATION_ID_CONFIG: {}", props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        props.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        LOG.info("Stream CLIENT_ID_CONFIG: {}", props.getProperty(StreamsConfig.CLIENT_ID_CONFIG));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (isDeployment) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credStorePassword);
        } else {
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
            props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
            props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, topic.getSerdeKey().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, topic.getSerdeValue().getClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        return props;
    }

    private Topic<String, Personhendelse> createConfiguredTopic(String topicName, String schemaRegistryUrl,
                                                                              String basicAuth) {
        var configuredTopic = new Topic<>(topicName, Serdes.String(), getSerde());
        if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
            var schemaMap =
                    Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                            AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, basicAuth,
                            SPECIFIC_AVRO_READER_CONFIG, true);
            configuredTopic.getSerdeKey().configure(schemaMap, true);
            configuredTopic.getSerdeValue().configure(schemaMap, false);
        }
        return configuredTopic;
    }

    private static String getBasicAuth(String schemaRegistryUsername, String schemaRegistryPassword) {
        return schemaRegistryUsername+":"+schemaRegistryPassword;
    }

    private Serde<Personhendelse> getSerde() {
        return isDeployment ? new SpecificAvroSerde<>() : new VtpKafkaAvroSerde<>();
    }
}
