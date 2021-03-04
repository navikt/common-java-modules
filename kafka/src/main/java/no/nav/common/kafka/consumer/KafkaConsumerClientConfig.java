package no.nav.common.kafka.consumer;

import java.util.Map;
import java.util.Properties;

public class KafkaConsumerClientConfig<K, V> {

    Properties properties;

    Map<String, TopicConsumer<K, V>> topicListeners;

}
