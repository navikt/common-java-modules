package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.*;

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
        return new JsonDeserializer<>(jsonClass);
    }

    public static <T> Deserializer<T> onPremAvroDeserializer(String schemaRegistryUrl, Class<T> clazz) {
        return null;
    }

    public static <T> Deserializer<T> aivenAvroDeserializer(Class<T> clazz) {
        String schemaRegistryUrl = getRequiredProperty(KAFKA_SCHEMA_REGISTRY);
        String username = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_USER);
        String password = getRequiredProperty(KAFKA_SCHEMA_REGISTRY_PASSWORD);

        return aivenAvroDeserializer(schemaRegistryUrl, username, password, clazz);
    }

    public static <T> Deserializer<T> aivenAvroDeserializer(String schemaRegistryUrl, String username, String password,  Class<T> clazz) {
        return (Deserializer<T>) new AvroDeserializer();
    }

}
