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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.toTopicConsumer;

@Slf4j
public class KafkaConsumerClientBuilder {

    private final List<TopicConfig<?, ?>> consumerTopicConfigs = new ArrayList<>();

    private Properties properties;

    private long pollDurationMs = -1;

    private Supplier<Boolean> isConsumerToggledOffSupplier;

    private KafkaConsumerClientBuilder() {}

    public static KafkaConsumerClientBuilder builder() {
        return new KafkaConsumerClientBuilder();
    }

    public KafkaConsumerClientBuilder withProperties(@NonNull Properties properties) {
        this.properties = (Properties) properties.clone();
        return this;
    }

    public KafkaConsumerClientBuilder withTopicConfig(TopicConfig<?, ?> topicConfig) {
        consumerTopicConfigs.add(topicConfig);
        return this;
    }

    public KafkaConsumerClientBuilder withTopicConfigs(List<TopicConfig<?, ?>> topicConfigs) {
        consumerTopicConfigs.addAll(topicConfigs);
        return this;
    }

    public KafkaConsumerClientBuilder withPollDuration(long pollDurationMs) {
        this.pollDurationMs = pollDurationMs;
        return this;
    }

    public KafkaConsumerClientBuilder withToggle(Supplier<Boolean> isToggledOffSupplier) {
        this.isConsumerToggledOffSupplier = isToggledOffSupplier;
        return this;
    }

    public KafkaConsumerClient build() {
        if (properties == null) {
            throw new IllegalStateException("Cannot build kafka consumer without properties");
        }

        Map<String, TopicConsumer<byte[], byte[]>> consumers = new HashMap<>();

        consumerTopicConfigs.forEach((consumerTopicConfig) -> {
            validateConfig(consumerTopicConfig);
            consumers.put(
                    consumerTopicConfig.getConsumerConfig().getTopic(),
                    createTopicConsumer(consumerTopicConfig)
            );
        });

        KafkaConsumerClientConfig<byte[], byte[]> config = new KafkaConsumerClientConfig<>(properties, consumers);

        if (pollDurationMs >= 0) {
            config.setPollDurationMs(pollDurationMs);
        }

        var client = new KafkaConsumerClientImpl<>(config);

        if (isConsumerToggledOffSupplier != null) {
            return new FeatureToggledKafkaConsumerClient(client, isConsumerToggledOffSupplier);
        }

        return client;
    }

    private static void validateConfig(TopicConfig<?, ?> consumerTopicConfig) {
        if (consumerTopicConfig.consumerConfig == null) {
            throw new IllegalStateException("Config is missing");
        }

        if (consumerTopicConfig.consumerConfig.topic == null) {
            throw new IllegalStateException("Topic is missing");
        }

        if (consumerTopicConfig.consumerConfig.keyDeserializer == null) {
            throw new IllegalStateException("Key deserializer is missing");
        }

        if (consumerTopicConfig.consumerConfig.valueDeserializer == null) {
            throw new IllegalStateException("Value deserializer is missing");
        }

        if (consumerTopicConfig.consumerConfig.consumer == null) {
            throw new IllegalStateException("Topic consumer is missing");
        }
    }

    public static <K, V> TopicConsumer<byte[], byte[]> createTopicConsumer(TopicConfig<K, V> consumerTopicConfig) {
        var listeners = consumerTopicConfig.getListeners();
        var config = consumerTopicConfig.getConsumerConfig();
        var consumerRepository = consumerTopicConfig.getConsumerRepository();

        TopicConsumer<byte[], byte[]> topicConsumer = (record) -> {
            ConsumerRecord<K, V> deserializedRecord = ConsumerUtils.deserializeConsumerRecord(
                    record,
                    config.getKeyDeserializer(),
                    config.getValueDeserializer()
            );

            ConsumeStatus status = ConsumerUtils.safeConsume(config.getConsumer(), deserializedRecord);

            if (!listeners.isEmpty()) {
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

    public static class TopicConfig<K, V> {

        private final List<TopicConsumerListener<K, V>> listeners = new ArrayList<>();

        private TopicConsumerConfig<K, V> consumerConfig;

        private KafkaConsumerRepository consumerRepository;

        public TopicConfig<K, V> withConsumerConfig(TopicConsumerConfig<K, V> consumerConfig) {
            this.consumerConfig = consumerConfig;
            return this;
        }

        public TopicConfig<K, V> withConsumerConfig(
                String topic,
                Deserializer<K> keyDeserializer,
                Deserializer<V> valueDeserializer,
                TopicConsumer<K, V> consumer
        ) {
            this.consumerConfig = new TopicConsumerConfig<>(topic, keyDeserializer, valueDeserializer, consumer);
            return this;
        }

        public TopicConfig<K, V> withConsumerConfig(
                String topic,
                Deserializer<K> keyDeserializer,
                Deserializer<V> valueDeserializer,
                Consumer<ConsumerRecord<K, V>> consumer
        ) {
            this.consumerConfig = new TopicConsumerConfig<>(topic, keyDeserializer, valueDeserializer, toTopicConsumer(consumer));
            return this;
        }

        public TopicConfig<K, V> withLogging() {
            listeners.add(new TopicConsumerLogger<>());
            return this;
        }

        public TopicConfig<K, V> withMetrics(MeterRegistry meterRegistry) {
            listeners.add(new TopicConsumerMetrics<>(meterRegistry));
            return this;
        }

        public TopicConfig<K, V> withStoreOnFailure(KafkaConsumerRepository consumerRepository) {
            this.consumerRepository = consumerRepository;
            return this;
        }

        public TopicConfig<K, V> withListener(TopicConsumerListener<K, V> consumerListener) {
            listeners.add(consumerListener);
            return this;
        }

        public TopicConfig<K, V> withListeners(List<TopicConsumerListener<K, V>> consumerListeners) {
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
