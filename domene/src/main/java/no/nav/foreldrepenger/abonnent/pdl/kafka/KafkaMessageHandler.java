package no.nav.foreldrepenger.abonnent.pdl.kafka;

import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public interface KafkaMessageHandler<K,V> {

    void handleRecord(K key, V value);

    // Configuration
    String topic();
    String groupId(); // Keep stable (or it will read from autoOffsetReset()
    default String autoOffsetReset() {
        return "earliest";
    }

    // Deserialization - should be configured (Avro). Provided as Supplier to handle Closeable
    Supplier<Deserializer<K>> keyDeserializer();
    Supplier<Deserializer<V>> valueDeserializer();

    interface KafkaStringMessageHandler extends KafkaMessageHandler<String, String> {

        default Supplier<Deserializer<String>> keyDeserializer() {
            return StringDeserializer::new;
        }

        default Supplier<Deserializer<String>> valueDeserializer() {
            return StringDeserializer::new;
        }
    }


}
