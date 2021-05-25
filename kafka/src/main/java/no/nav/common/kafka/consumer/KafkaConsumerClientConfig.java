package no.nav.common.kafka.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.common.kafka.consumer.util.ConsumerClientExceptionListener;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.KafkaConsumerClientImpl.DEFAULT_POLL_DURATION_MS;

@AllArgsConstructor
@Data
public class KafkaConsumerClientConfig<K, V> {

    Properties properties;

    Map<String, TopicConsumer<K, V>> topics;

    long pollDurationMs;

    List<ConsumerClientExceptionListener> exceptionListeners;

    public KafkaConsumerClientConfig(Properties properties, Map<String, TopicConsumer<K, V>> topics) {
        this.properties = properties;
        this.topics = topics;
        this.pollDurationMs = DEFAULT_POLL_DURATION_MS;
    }

}
