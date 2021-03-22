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

    public final static int DEFAULT_MAX_POLL_RECORDS = 500;

    public final static long DEFAULT_LINGER_MS = 500;

    public static ConsumerPropertiesBuilder consumerBuilder() {
        return new ConsumerPropertiesBuilder();
    }

    public static ProducerPropertiesBuilder producerBuilder() {
        return new ProducerPropertiesBuilder();
    }

    public abstract static class BasePropertiesBuilder<T extends BasePropertiesBuilder<?>> {

        final Properties properties = new Properties();

        public T withBrokerUrl(String brokerUrl) {
            properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
            return (T) this;
        }

        public T withOnPremAuth(String username, String password) {
            return withOnPremAuth(new Credentials(username, password));
        }

        public T withOnPremAuth(Credentials credentials) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + credentials.username + "\" password=\"" + credentials.password + "\";");
            return (T) this;
        }

        public T withProp(String key, Object value) {
            properties.put(key, value);
            return (T) this;
        }

        public T withProps(Properties properties) {
            this.properties.putAll(properties);
            return (T) this;
        }

        public Properties build() {
            if (!properties.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
                throw new IllegalStateException(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " is missing");
            }

            return properties;
        }

    }

    public static class ProducerPropertiesBuilder extends BasePropertiesBuilder<ProducerPropertiesBuilder> {

        /**
         * Optional id used for logging by kafka
         * @param producerId the name of the instance that is producing records
         * @return the producer properties builder
         */
        public ProducerPropertiesBuilder withProducerId(String producerId) {
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, producerId);
            return this;
        }

        public ProducerPropertiesBuilder withBaseProperties() {
            properties.put(ProducerConfig.ACKS_CONFIG, "all");
            properties.put(ProducerConfig.LINGER_MS_CONFIG, DEFAULT_LINGER_MS);
            return this;
        }

        public <K extends Serializer<?>, V extends Serializer<?>> ProducerPropertiesBuilder withSerializers(Class<K> keySerializerClass, Class<V> valueSerializerClass) {
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
            return this;
        }

        public Properties build() {
            Object keySerializer = properties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
            Object valueSerializer = properties.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);

            if (keySerializer == null) {
                throw new IllegalStateException("Key serializer is missing");
            } else if (valueSerializer == null) {
                throw new IllegalStateException("Value serializer is missing");
            }

            return super.build();
        }

    }

    public static class ConsumerPropertiesBuilder extends BasePropertiesBuilder<ConsumerPropertiesBuilder> {

        public ConsumerPropertiesBuilder withConsumerGroupId(String consumerGroupId) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
            return this;
        }

        public ConsumerPropertiesBuilder withBaseProperties() {
            return withBaseProperties(DEFAULT_MAX_POLL_RECORDS);
        }

        public ConsumerPropertiesBuilder withBaseProperties(int maxRecordsPrBatch) {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            return withPollProperties(maxRecordsPrBatch, 1500 * maxRecordsPrBatch);
        }

        /**
         * Sets how many records can be consumed for each batch when polling, and how long kafka will wait between each poll before marking the client as failed.
         * @param maxRecordsPrBatch max number of records that will be consumed for each batch
         * @param maxWaitForEachBatch how long kafka will wait for the consumer to finish each batch
         * @return the consumer properties builder
         */
        public ConsumerPropertiesBuilder withPollProperties(int maxRecordsPrBatch, long maxWaitForEachBatch) {
            properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxRecordsPrBatch);
            properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxWaitForEachBatch);
            return this;
        }

        public <K extends Deserializer<?>, V extends Deserializer<?>> ConsumerPropertiesBuilder withDeserializers(Class<K> keyDeserializerClass, Class<V> valueDeserializerClass) {
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
            return this;
        }

        public Properties build() {
            Object keyDeserializer = properties.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
            Object valueDeserializer = properties.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);

            if (keyDeserializer == null) {
                throw new IllegalStateException("Key deserializer is missing");
            } else if (valueDeserializer == null) {
                throw new IllegalStateException("Value deserializer is missing");
            }

            return super.build();
        }

    }

}
