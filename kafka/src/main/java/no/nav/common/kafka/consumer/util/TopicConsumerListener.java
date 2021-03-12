package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Listener which receives a consumed record and status from a {@link no.nav.common.kafka.consumer.TopicConsumer}
 * @param <K> topic key
 * @param <V> topic value
 */
public interface TopicConsumerListener<K, V> {

    void onConsumed(ConsumerRecord<K, V> record, ConsumeStatus status);

}
