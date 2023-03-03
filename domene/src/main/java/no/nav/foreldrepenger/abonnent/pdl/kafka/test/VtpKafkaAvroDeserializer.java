package no.nav.foreldrepenger.abonnent.pdl.kafka.test;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import no.nav.person.pdl.leesah.Personhendelse;

import org.apache.avro.Schema;

public class VtpKafkaAvroDeserializer extends KafkaAvroDeserializer {

    @Override
    public Object deserialize(String topic, byte[] bytes) {
        this.schemaRegistry = getMockClient(Personhendelse.SCHEMA$);
        return super.deserialize(topic, bytes);
    }

    private static SchemaRegistryClient getMockClient(final Schema schema$) {
        return new MockSchemaRegistryClient() {
            @Override
            public synchronized AvroSchema getSchemaBySubjectAndId(String subject, int id) {
                return new AvroSchema(schema$);
            }
        };
    }
}
