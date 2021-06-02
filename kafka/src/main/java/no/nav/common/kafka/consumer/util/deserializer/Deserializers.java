package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.*;

import java.util.List;
import java.util.UUID;

import static no.nav.common.kafka.util.KafkaEnvironmentVariables.*;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

public class Deserializers {

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
        return new JsonObjectDeserializer<>(jsonClass);
    }

    public static <T> Deserializer<List<T>> jsonArrayDeserializer(Class<T> jsonClass) {
        return new JsonArrayDeserializer<>(jsonClass);
    }

    public static <T> Deserializer<T> onPremAvroDeserializer(String schemaRegistryUrl) {
        return new AvroDeserializer<>(schemaRegistryUrl);
    }

    public static <T> Deserializer<T> aivenAvroDeserializer() {
        String schemaRegistryUrl = getRequiredProperty(KAFKA_SCHEMA_REGISTRY);
        String username = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_USER);
        String password = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_PASSWORD);

        return new AvroDeserializer<>(schemaRegistryUrl, username, password);
    }

}
