package no.nav.common.kafka.util;

import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Properties;

public class KafkaPropertiesBuilder {

    public final static int DEFAULT_MAX_POLL_RECORDS = 1000;

    private Properties properties;

    private KafkaPropertiesBuilder() {
        properties = new Properties();
    }

    public static KafkaPropertiesBuilder builder() {
        return new KafkaPropertiesBuilder();
    }

    public KafkaPropertiesBuilder withBrokerUrl(String brokerUrl) {
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        return this;
    }

    public KafkaPropertiesBuilder withConsumerGroupId(String consumerGroupId) {
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        return this;
    }

    public KafkaPropertiesBuilder withProducerId(String producerId) {
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, producerId);
        return this;
    }

    public KafkaPropertiesBuilder withBaseProducerProperties() {
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 500);
        return this;
    }

    public KafkaPropertiesBuilder withBaseConsumerProperties() {
        return withBaseConsumerProperties(DEFAULT_MAX_POLL_RECORDS);
    }

    public KafkaPropertiesBuilder withBaseConsumerProperties(int maxRecordsPrBatch) {
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000 * maxRecordsPrBatch);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxRecordsPrBatch);
        return this;
    }

    public KafkaPropertiesBuilder withOnPremAuth(String username, String password) {
        return withOnPremAuth(new Credentials(username, password));
    }

    public KafkaPropertiesBuilder withOnPremAuth(Credentials credentials) {
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + credentials.username + "\" password=\"" + credentials.password + "\";");
        return this;
    }

    public  <K extends Deserializer<?>, V extends Deserializer<?>> KafkaPropertiesBuilder withConsumerDeserializers(Class<K> keyDeserializerClass, Class<V> valueDeserializerClass) {
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
        return this;
    }

    public <K extends Serializer<?>, V extends Serializer<?>> KafkaPropertiesBuilder withProducerSerializers(Class<K> keySerializerClass, Class<V> valueSerializerClass) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        return this;
    }

    public KafkaPropertiesBuilder withProp(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public KafkaPropertiesBuilder withProps(Properties properties) {
        this.properties.putAll(properties);
        return this;
    }

    public Properties build() {
        if (!properties.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new IllegalStateException(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " is missing");
        }

        Object keyDeserializer = properties.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
        Object valueDeserializer = properties.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);

        Object keySerializer = properties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        Object valueSerializer = properties.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);

        if ((keyDeserializer != null && valueDeserializer == null) || (valueDeserializer != null && keyDeserializer == null)) {
            throw new IllegalStateException("Must set both key deserializer and value deserializer");
        }

        if ((keySerializer != null && valueSerializer == null) || (valueSerializer != null && keySerializer == null)) {
            throw new IllegalStateException("Must set both key serializer and value serializer");
        }

        if ((keyDeserializer != null) == (keySerializer != null)) {
            throw new IllegalStateException("Cannot set both key serializer and key deserializer");
        }

        return properties;
    }

}
