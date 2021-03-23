package no.nav.common.kafka.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.KafkaConsumerClient.DEFAULT_POLL_DURATION_MS;

@AllArgsConstructor
@Data
public class KafkaConsumerClientConfig<K, V> {

    Properties properties;

    Map<String, TopicConsumer<K, V>> topics;

    long pollDurationMs;

    public KafkaConsumerClientConfig(Properties properties, Map<String, TopicConsumer<K, V>> topics) {
        this.properties = properties;
        this.topics = topics;
        this.pollDurationMs = DEFAULT_POLL_DURATION_MS;
    }

}
