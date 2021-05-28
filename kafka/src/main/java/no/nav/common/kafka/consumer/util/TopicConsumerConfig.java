package no.nav.common.kafka.consumer.util;

import lombok.Value;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.common.serialization.Deserializer;

@Value
public class TopicConsumerConfig<K, V> {

    String topic;

    Deserializer<K> keyDeserializer;

    Deserializer<V> valueDeserializer;

    TopicConsumer<K, V> consumer;

}
