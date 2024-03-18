package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import no.nav.foreldrepenger.konfig.Environment;

public class LokalKafkaProperties {

    private static final Environment ENV = Environment.current();
    private static final boolean IS_DEPLOYMENT = ENV.isProd() || ENV.isDev();
    private static final String APPLICATION_NAME = ENV.getNaisAppName();

    private LokalKafkaProperties() {
    }

    // Alle som konsumerer Json-meldinger
    public static Properties forConsumerStringValue(String groupId) {
        return forConsumerGenericValue(groupId, new StringDeserializer(), new StringDeserializer(), "earliest");
    }

    public static <K,V> Properties forConsumerGenericValue(String groupId, Deserializer<K> valueKey, Deserializer<V> valueSerde, String offsetReset) {
        final Properties props = new Properties();

        props.put(CommonClientConfigs.GROUP_ID_CONFIG, groupId);
        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, generateClientId());
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getAivenConfig("KAFKA_BROKERS"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

        putSecurity(props);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, valueKey.getClass());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueSerde.getClass());

        // Polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // Unngå store Tx dersom alle prosesseres innen samme Tx. Default 500
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "100000"); // Gir inntil 1s pr record. Default er 600 ms/record

        return props;
    }


    private static String getAivenConfig(String property) {
        return Optional.ofNullable(ENV.getProperty(property))
            .orElseGet(() -> ENV.getProperty(property.toLowerCase().replace('_', '.')));
    }

    private static String generateClientId() {
        return APPLICATION_NAME + "-" + UUID.randomUUID();
    }

    private static void putSecurity(Properties props) {
        if (IS_DEPLOYMENT) {
            var credStorePassword = getAivenConfig("KAFKA_CREDSTORE_PASSWORD");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, getAivenConfig("KAFKA_TRUSTSTORE_PATH"));
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credStorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getAivenConfig("KAFKA_KEYSTORE_PATH"));
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credStorePassword);
        } else {
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
            props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, "vtp", "vtp");
            props.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }
    }


}
