package no.nav.common.kafka.consumer.feilhandtering;

import lombok.Data;

@Data
public class KafkaConsumerRecord<K, V> {
    private final long id;
    private final String topic;
    private final int partition;
    private final long offset;
    private final K key;
    private final V value;

    public KafkaConsumerRecord(long id, String topic, int partition, long offset, K key, V value) {
        this.id = id;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

}
