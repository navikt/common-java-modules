package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener which receives a consumed record and status from a {@link no.nav.common.kafka.consumer.TopicConsumer}
 * @param <K> topic key
 * @param <V> topic value
 */
public class TopicConsumerLogger<K, V> implements TopicConsumerListener<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onConsumed(ConsumerRecord<K, V> record, ConsumeStatus status) {
        if (status == ConsumeStatus.OK) {
            log.info("Consumed record topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
        } else {
            log.error("Failed to consume record topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
        }
    }

}
