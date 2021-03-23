package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoreOnFailureTopicConsumer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaConsumerClientBuilder<K, V> {

    private final Map<String, TopicConsumer<K, V>> consumerMap = new HashMap<>();

    private final Map<String, TopicConsumer<K, V>> consumersWithErrorHandlingMap = new HashMap<>();

    private Properties properties;

    private long pollDurationMs = -1;

    private KafkaConsumerRepository consumerRepository;

    private Serializer<K> keySerializer;

    private Serializer<V> valueSerializer;

    private boolean enableLogging;

    private MeterRegistry meterRegistry;

    private KafkaConsumerClientBuilder() {}

    public static <K, V> KafkaConsumerClientBuilder<K, V> builder() {
        return new KafkaConsumerClientBuilder<K, V>();
    }

    public KafkaConsumerClientBuilder<K, V> withProps(Properties properties) {
        this.properties = properties;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withConsumer(String topic, TopicConsumer<K, V> consumer) {
        consumerMap.put(topic, consumer);
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withConsumers(Map<String, TopicConsumer<K, V>> topicConsumers) {
        consumerMap.putAll(topicConsumers);
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withStoreOnFailureConsumer(String topic, TopicConsumer<K, V> topicConsumer) {
        consumersWithErrorHandlingMap.put(topic, topicConsumer);
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withStoreOnFailureConsumers(Map<String, TopicConsumer<K, V>> topicConsumers) {
        consumersWithErrorHandlingMap.putAll(topicConsumers);
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withLogging() {
        this.enableLogging = true;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withRepository(KafkaConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withSerializers(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withPollDuration(long pollDurationMs) {
        this.pollDurationMs = pollDurationMs;
        return this;
    }

    public KafkaConsumerClient<K, V> build() {
        if (properties == null) {
            throw new IllegalStateException("Cannot build kafka consumer without properties");
        }

        if (!consumersWithErrorHandlingMap.isEmpty()) {
            if (consumerRepository == null) {
                throw new IllegalStateException("Consumer repository is required when using error handling");
            }

            if (keySerializer == null || valueSerializer == null) {
                throw new IllegalStateException("Key serializer and value serializer is required when using error handling");
            }
        }

        consumersWithErrorHandlingMap.forEach((topic, consumer) -> {
            consumerMap.put(topic, new StoreOnFailureTopicConsumer<>(consumer, consumerRepository, keySerializer, valueSerializer));
        });

        Map<String, TopicConsumer<K, V>> extendedConsumers = new HashMap<>();

        consumerMap.forEach((topic, consumer) -> {
            TopicConsumerBuilder<K, V> builder = TopicConsumerBuilder.builder();

            if (enableLogging) {
                builder.withLogging();
            }

            if (meterRegistry != null) {
                builder.withMetrics(meterRegistry);
            }

            builder.withConsumer(consumer);

            extendedConsumers.put(topic, builder.build());
        });

        KafkaConsumerClientConfig<K, V> config = new KafkaConsumerClientConfig<>(properties, extendedConsumers);

        if (pollDurationMs >= 0) {
            config.setPollDurationMs(pollDurationMs);
        }

        return new KafkaConsumerClient<>(config);
    }

}
