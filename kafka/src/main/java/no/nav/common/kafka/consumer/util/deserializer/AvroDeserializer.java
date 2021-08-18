package no.nav.common.kafka.consumer.util.deserializer;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class AvroDeserializer<T> implements Deserializer<T> {

    private final static Map<String, SchemaRegistryClient> SCHEMA_REGISTRY_CLIENTS = new HashMap<>();

    private final static int SCHEMA_MAP_CAPACITY = 100;

    private final KafkaAvroDeserializer kafkaAvroDeserializer;

    public AvroDeserializer(String schemaRegistryUrl, String username, String password) {
        SchemaRegistryClient schemaRegistryClient = SCHEMA_REGISTRY_CLIENTS.computeIfAbsent(
                schemaRegistryUrl,
                (url) -> {
                    Map<String, Object> configs = new HashMap<>();

                    configs.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
                    configs.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, format("%s:%s", username, password));

                    return new CachedSchemaRegistryClient(schemaRegistryUrl, SCHEMA_MAP_CAPACITY, configs);
                }
        );

        kafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient);
    }

    public AvroDeserializer(String schemaRegistryUrl) {
        this(schemaRegistryUrl, Map.of());
    }

    public AvroDeserializer(String schemaRegistryUrl, Map<String, ?> props) {
        SchemaRegistryClient schemaRegistryClient = SCHEMA_REGISTRY_CLIENTS.computeIfAbsent(
                schemaRegistryUrl,
                (url) -> new CachedSchemaRegistryClient(schemaRegistryUrl, SCHEMA_MAP_CAPACITY)
        );

        kafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient, props);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        return (T) kafkaAvroDeserializer.deserialize(topic, data);
    }

}
