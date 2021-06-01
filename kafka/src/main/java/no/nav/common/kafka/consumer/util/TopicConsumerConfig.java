package no.nav.common.kafka.consumer.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.common.serialization.Deserializer;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicConsumerConfig<K, V> {

    String topic;

    Deserializer<K> keyDeserializer;

    Deserializer<V> valueDeserializer;

    TopicConsumer<K, V> consumer;

}
