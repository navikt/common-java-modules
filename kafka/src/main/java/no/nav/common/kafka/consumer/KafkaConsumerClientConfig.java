package no.nav.common.kafka.consumer;

import java.util.Map;
import java.util.Properties;

public class KafkaConsumerClientConfig {

    String groupId;

    Properties properties;

    Map<String, KafkaConsumerListener> topicListeners;

}
