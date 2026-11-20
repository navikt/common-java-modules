package no.nav.common.kafka.util;

/**
 * Environment variables that are injected into the application when using Aiven Kafka on NAIS
 * See: https://doc.nais.io/addons/kafka/#application-config
 */
public class KafkaEnvironmentVariables {

    public static final String KAFKA_BROKERS = "KAFKA_BROKERS";

    public static final String KAFKA_SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY";

    public static final String KAFKA_SCHEMA_REGISTRY_USER = "KAFKA_SCHEMA_REGISTRY_USER";

    public static final String KAFKA_SCHEMA_REGISTRY_PASSWORD = "KAFKA_SCHEMA_REGISTRY_PASSWORD";

    public static final String KAFKA_CERTIFICATE = "KAFKA_CERTIFICATE";

    public static final String KAFKA_CERTIFICATE_PATH = "KAFKA_CERTIFICATE_PATH";

    public static final String KAFKA_PRIVATE_KEY = "KAFKA_PRIVATE_KEY";

    public static final String KAFKA_PRIVATE_KEY_PATH = "KAFKA_PRIVATE_KEY_PATH";

    public static final String KAFKA_CA = "KAFKA_CA";

    public static final String KAFKA_CA_PATH = "KAFKA_CA_PATH";

    public static final String KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD";

    public static final String KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH";

    public static final String KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH";
}
