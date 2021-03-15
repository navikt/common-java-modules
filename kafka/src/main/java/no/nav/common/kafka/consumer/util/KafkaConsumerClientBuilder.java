package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.TopicConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaConsumerClientBuilder<K, V> {

    private Properties properties;

    private long pollDurationMs = -1;

    private final Map<String, TopicConsumer<K, V>> consumerMap = new HashMap<>();

    private KafkaConsumerClientBuilder() {}

    public static <K, V> KafkaConsumerClientBuilder<K, V> builder() {
        return new KafkaConsumerClientBuilder<K, V>();
    }

    public KafkaConsumerClientBuilder<K, V> withProps(Properties properties) {
        this.properties = properties;
        return this;
    }

    public KafkaConsumerClientBuilder<K, V> withTopic(String topic, TopicConsumer<K, V> consumer) {
        consumerMap.put(topic, consumer);
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

        KafkaConsumerClientConfig<K, V> config = new KafkaConsumerClientConfig<>(properties, consumerMap);

        if (pollDurationMs >= 0) {
            config.setPollDurationMs(pollDurationMs);
        }

        return new KafkaConsumerClient<>(config);
    }

}
