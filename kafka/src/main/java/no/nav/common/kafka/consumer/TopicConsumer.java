package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface TopicConsumer<K, V> {

    ConsumeStatus consume(ConsumerRecord<K, V> record);

}
