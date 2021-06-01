package no.nav.common.kafka.consumer.util.deserializer;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.serialization.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.common.kafka.util.KafkaEnvironmentVariables.*;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

public class Deserializers {

    private final static Map<String, SchemaRegistryClient> SCHEMA_REGISTRY_CLIENTS = new HashMap<>();

    private final static int SCHEMA_MAP_CAPACITY = 100;

    public static Deserializer<String> stringDeserializer() {
        return new StringDeserializer();
    }

    public static Deserializer<Long> longDeserializer() {
        return new LongDeserializer();
    }

    public static Deserializer<Short> shortDeserializer() {
        return new ShortDeserializer();
    }

    public static Deserializer<Double> doubleDeserializer() {
        return new DoubleDeserializer();
    }

    public static Deserializer<Float> floatDeserializer() {
        return new FloatDeserializer();
    }

    public static Deserializer<Integer> integerDeserializer() {
        return new IntegerDeserializer();
    }

    public static Deserializer<UUID> uuidDeserializer() {
        return new UUIDDeserializer();
    }

    public static <T> Deserializer<T> jsonDeserializer(Class<T> jsonClass) {
        return new JsonDeserializer<>(jsonClass);
    }

    public static <T> Deserializer<T> onPremAvroDeserializer(String schemaRegistryUrl) {
        return avroDeserializer(schemaRegistryUrl);
    }

    public static <T> Deserializer<T> aivenAvroDeserializer() {
        String schemaRegistryUrl = getRequiredProperty(KAFKA_SCHEMA_REGISTRY);
        String username = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_USER);
        String password = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_PASSWORD);

        return avroDeserializer(schemaRegistryUrl, username, password);
    }

    public static <T> Deserializer<T> avroDeserializer(String schemaRegistryUrl) {
        SchemaRegistryClient schemaRegistryClient = SCHEMA_REGISTRY_CLIENTS.computeIfAbsent(
                schemaRegistryUrl,
                (url) -> new CachedSchemaRegistryClient(schemaRegistryUrl, SCHEMA_MAP_CAPACITY)
        );

        return (Deserializer<T>) new KafkaAvroDeserializer(schemaRegistryClient);
    }

    public static <T> Deserializer<T> avroDeserializer(String schemaRegistryUrl, String username, String password) {
        SchemaRegistryClient schemaRegistryClient = SCHEMA_REGISTRY_CLIENTS.computeIfAbsent(
                schemaRegistryUrl,
                (url) -> {
                    Map<String, Object> configs = new HashMap<>();

                    configs.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
                    configs.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, format("%s:%s", username, password));

                    return new CachedSchemaRegistryClient(schemaRegistryUrl, SCHEMA_MAP_CAPACITY, configs);
                }
        );

        return (Deserializer<T>) new KafkaAvroDeserializer(schemaRegistryClient);
    }

}
