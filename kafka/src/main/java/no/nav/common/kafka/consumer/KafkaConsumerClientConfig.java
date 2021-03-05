package no.nav.common.kafka.consumer;

import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.KafkaConsumerClient.DEFAULT_POLL_DURATION_MS;

public class KafkaConsumerClientConfig<K, V> {

    long pollDurationMs = DEFAULT_POLL_DURATION_MS;

    Properties properties;

    Map<String, TopicConsumer<K, V>> topics;

}
