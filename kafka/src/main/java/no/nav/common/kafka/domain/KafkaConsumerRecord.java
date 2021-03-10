package no.nav.common.kafka.domain;

public class KafkaConsumerRecord<K, V> {
    public final long id;
    public final String topic;
    public final K key;
    public final V value;
    public final long offset;
    public final int partition;

    public KafkaConsumerRecord(long id, String topic, K key, V value, long offset, int partition) {
        this.id = id;
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.offset = offset;
        this.partition = partition;
    }
}
