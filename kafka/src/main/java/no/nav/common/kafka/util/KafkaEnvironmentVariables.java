package no.nav.common.kafka.util;

/**
 * Environment variables that are injected into the application when using Aiven Kafka on NAIS
 * See: https://doc.nais.io/addons/kafka/#application-config
 */
public class KafkaEnvironmentVariables {

    public final static String KAFKA_BROKERS = "KAFKA_BROKERS";

    public final static String KAFKA_SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY";

    public final static String KAFKA_SCHEMA_REGISTRY_USER = "KAFKA_SCHEMA_REGISTRY_USER";

    public final static String KAFKA_SCHEMA_REGISTRY_PASSWORD = "KAFKA_SCHEMA_REGISTRY_PASSWORD";

    public final static String KAFKA_CERTIFICATE = "KAFKA_CERTIFICATE";

    public final static String KAFKA_CERTIFICATE_PATH = "KAFKA_CERTIFICATE_PATH";

    public final static String KAFKA_PRIVATE_KEY = "KAFKA_PRIVATE_KEY";

    public final static String KAFKA_PRIVATE_KEY_PATH = "KAFKA_PRIVATE_KEY_PATH";

    public final static String KAFKA_CA = "KAFKA_CA";

    public final static String KAFKA_CA_PATH = "KAFKA_CA_PATH";

    public final static String KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD";

    public final static String KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH";

    public final static String KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH";

}
