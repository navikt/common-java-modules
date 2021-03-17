package no.nav.common.kafka.consumer.feilhandtering;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class KafkaConsumerRecord<K, V> {
    private final long id;
    private final String topic;
    private final int partition;
    private final long offset;
    private final K key;
    private final V value;
    private final int retries;
    private final Timestamp lastRetry;
}
