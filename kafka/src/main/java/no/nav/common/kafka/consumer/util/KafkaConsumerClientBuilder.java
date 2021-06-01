package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.*;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoreOnFailureTopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.*;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.deserializeConsumerRecord;

@Slf4j
public class KafkaConsumerClientBuilder {

    private final List<Config<?, ?>> consumerConfigs = new ArrayList<>();
    
    private Properties properties;

    private Properties additionalProperties;

    private long pollDurationMs = -1;

    private boolean useRollingCredentials;

    private KafkaConsumerClientBuilder() {}

    public static KafkaConsumerClientBuilder builder() {
        return new KafkaConsumerClientBuilder();
    }

    public KafkaConsumerClientBuilder withProperties(@NonNull Properties properties) {
        this.properties = (Properties) properties.clone();
        return this;
    }

    /**
     * Adds additional properties that will overwrite properties from {@link #withProperties(Properties)}.
     * Useful for configuring a consumer with additional properties when using a preset from
     * {@link no.nav.common.kafka.util.KafkaPropertiesPreset} as the base.
     * @param properties additional properties
     * @return this builder
     */
    public KafkaConsumerClientBuilder withAdditionalProperties(@NonNull Properties properties) {
        this.additionalProperties = (Properties) properties.clone();
        return this;
    }

    /**
     * Adds an additional property that will overwrite properties from {@link #withProperties(Properties)}.
     * Useful for configuring a consumer with additional properties when using a preset from
     * {@link no.nav.common.kafka.util.KafkaPropertiesPreset} as the base.
     * @return this builder
     */
    public KafkaConsumerClientBuilder withAdditionalProperty(@NonNull String name, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new Properties();
        }

        additionalProperties.put(name, value);
        return this;
    }

    public KafkaConsumerClientBuilder withConfig(Config<?, ?> config) {
        consumerConfigs.add(config);
        return this;
    }

    public KafkaConsumerClientBuilder withConfigs(List<Config<?, ?>> configs) {
        consumerConfigs.addAll(configs);
        return this;
    }

    public KafkaConsumerClientBuilder withPollDuration(long pollDurationMs) {
        this.pollDurationMs = pollDurationMs;
        return this;
    }

    public KafkaConsumerClientBuilder withRollingCredentials() {
        useRollingCredentials = true;
        return this;
    }

    public KafkaConsumerClient build() {
        if (properties == null) {
            throw new IllegalStateException("Cannot build kafka consumer without properties");
        }

        Map<String, TopicConsumer<byte[], byte[]>> consumers = new HashMap<>();

        consumerConfigs.forEach((consumerConfig) -> {
            // TODO: Validate config
            consumers.put(
                    consumerConfig.getConsumerConfig().getTopic(),
                    createTopicConsumer(consumerConfig)
            );
        });

        KafkaConsumerClientConfig<byte[], byte[]> config = new KafkaConsumerClientConfig<>(properties, consumers);

        if (pollDurationMs >= 0) {
            config.setPollDurationMs(pollDurationMs);
        }

        if (useRollingCredentials) {
            return new RollingCredentialsKafkaConsumerClient(config);
        }

        return new KafkaConsumerClientImpl<>(config);
    }

    public static <K, V> TopicConsumer<byte[], byte[]> createTopicConsumer(Config<K, V> consumerConfig) {
        var listeners = consumerConfig.getListeners();
        var config = consumerConfig.getConsumerConfig();
        var consumerRepository = consumerConfig.getConsumerRepository();

        TopicConsumer<byte[], byte[]> topicConsumer = (record) -> {
            ConsumeStatus status = ConsumerUtils.safeConsume(ConsumerUtils.createTopicConsumer(config), record);

            if (!listeners.isEmpty()) {
                ConsumerRecord<K, V> deserializedRecord = deserializeConsumerRecord(
                        record,
                        config.getKeyDeserializer(),
                        config.getValueDeserializer()
                );

                listeners.forEach(listener -> {
                    try {
                        listener.onConsumed(deserializedRecord, status);
                    } catch (Exception e) {
                        log.error("Caught exception from consumer listener", e);
                    }
                });
            }

            return status;
        };

        if (consumerRepository != null) {
            return new StoreOnFailureTopicConsumer(topicConsumer, consumerRepository);
        }

        return topicConsumer;
    }
    
    public static class Config<K, V> {

        private final List<TopicConsumerListener<K, V>> listeners = new ArrayList<>();

        private TopicConsumerConfig<K, V> consumerConfig;

        private KafkaConsumerRepository consumerRepository;

        public Config<K, V> withConsumerConfig(TopicConsumerConfig<K, V> consumerConfig) {
            this.consumerConfig = consumerConfig;
            return this;
        }

        public Config<K, V> withConsumerConfig(String topic, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, TopicConsumer<K, V> consumer) {
            this.consumerConfig = new TopicConsumerConfig<>(topic, keyDeserializer, valueDeserializer, consumer);
            return this;
        }

        public Config<K, V> withLogging() {
            listeners.add(new TopicConsumerLogger<>());
            return this;
        }

        public Config<K, V> withMetrics(MeterRegistry meterRegistry) {
            listeners.add(new TopicConsumerMetrics<>(meterRegistry));
            return this;
        }

        public Config<K, V> withStoreOnFailure(KafkaConsumerRepository consumerRepository) {
            this.consumerRepository = consumerRepository;
            return this;
        }

        public Config<K, V> withListener(TopicConsumerListener<K, V> consumerListener) {
            listeners.add(consumerListener);
            return this;
        }

        public Config<K, V> withListeners(List<TopicConsumerListener<K, V>> consumerListeners) {
            listeners.addAll(consumerListeners);
            return this;
        }

        public List<TopicConsumerListener<K, V>> getListeners() {
            return listeners;
        }

        public TopicConsumerConfig<K, V> getConsumerConfig() {
            return consumerConfig;
        }

        public KafkaConsumerRepository getConsumerRepository() {
            return consumerRepository;
        }

    }

}
