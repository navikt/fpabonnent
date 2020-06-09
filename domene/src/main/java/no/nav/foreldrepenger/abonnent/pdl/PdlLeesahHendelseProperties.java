package no.nav.foreldrepenger.abonnent.pdl;

import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.env.Environment;

@Dependent
public class PdlLeesahHendelseProperties {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseProperties.class);

    private static final String KAFKA_AVRO_SERDE_CLASS = "kafka.avro.serde.class";

    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final Topic<String, Personhendelse> personhendelseTopic;
    private final String username;
    private final String password;
    private final String applicationId;
    private final String trustStorePath;
    private final String trustStorePassword;

    @Inject
    public PdlLeesahHendelseProperties(@KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                                       @KonfigVerdi("kafka.schema.registry.url") String schemaRegistry,
                                       @KonfigVerdi("systembruker.username") String username,
                                       @KonfigVerdi("systembruker.password") String password,
                                       @KonfigVerdi(value = "javax.net.ssl.trustStore") String trustStorePath,
                                       @KonfigVerdi(value = "javax.net.ssl.trustStorePassword") String trustStorePassword,
                                       @KonfigVerdi("kafka.topic.pdl.leesah.hendelse") String topic,
                                       @KonfigVerdi(value = KAFKA_AVRO_SERDE_CLASS, defaultVerdi = "io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde") String kafkaAvroSerdeClass) {
        this.personhendelseTopic = new Topic<>(topic, Serdes.String(), getSerde(kafkaAvroSerdeClass));
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistry;
        this.username = username;
        this.password = password;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;

        String namespace = Environment.current().getNamespace().getNamespace();
        LOG.info("Namespace fra Environment: {}", namespace); //TODO(JEJ) Fjerne når testet i Q
        if (namespace == null || namespace.isBlank()) {
            namespace = getEnvVar("NAIS_NAMESPACE", "default");
        }
        this.applicationId = getEnvVar("NAIS_APP_NAME", "fpabonnent") + "-" + namespace;
    }

    public Topic<String, Personhendelse> getTopic() {
        return personhendelseTopic;
    }

    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    String getApplicationId() {
        return applicationId;
    }

    private boolean harSattTrustStore() {
        return trustStorePath != null && !trustStorePath.isEmpty()
                && trustStorePassword != null && !trustStorePassword.isEmpty();
    }

    private String getTrustStorePath() {
        return trustStorePath;
    }

    private String getTrustStorePassword() {
        return trustStorePassword;
    }

    public Properties getProperties() {
        final Properties props = new Properties();

        /*
         * Application ID må være unik per strøm for å unngå en feilsituasjon der
         * man enkelte ganger får feil partition (dvs partitions fra annen topic
         * enn den man skal ha).
         */
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, getApplicationId() + "-" + personhendelseTopic.getConsumerClientId());
        LOG.info("Stream APPLICATION_ID_CONFIG: {}", props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        props.put(StreamsConfig.CLIENT_ID_CONFIG, personhendelseTopic.getConsumerClientId());
        LOG.info("Stream CLIENT_ID_CONFIG: {}", props.getProperty(StreamsConfig.CLIENT_ID_CONFIG));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());

        // Sikkerhet
        if (harSattBrukernavn()) {
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(jaasTemplate, getUsername(), getPassword()));
        }

        if (harSattTrustStore()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getTrustStorePath());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getTrustStorePassword());
        }

        // Setup schema-registry
        if (getSchemaRegistryUrl() != null) {
            props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, getSchemaRegistryUrl());
        }

        // Serde
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, personhendelseTopic.getSerdeKey().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, personhendelseTopic.getSerdeValue().getClass());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);

        return props;
    }

    private Serde getSerde(@KonfigVerdi(KAFKA_AVRO_SERDE_CLASS) String kafkaAvroSerdeClass) {
        Serde serde = new SpecificAvroSerde<>();
        if (kafkaAvroSerdeClass != null && !kafkaAvroSerdeClass.isBlank()) {
            try {
                serde = (Serde)Class.forName(kafkaAvroSerdeClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                LOG.warn(String.format("Utvikler-feil: Konfigurasjonsverdien '%s' peker på klasse '%s' som ikke kunne brukes. Benytter default.", KAFKA_AVRO_SERDE_CLASS, kafkaAvroSerdeClass), e);
            }
        }
        return serde;
    }

    private String getEnvVar(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val == null) ? defaultValue : val;
    }
}
