package no.nav.common.kafka.domain;

public class KafkaConsumerRecord<K, V> {
    public final long id;
    public final String topic;
    public final int partition;
    public final long offset;
    public final K key;
    public final V value;

    public KafkaConsumerRecord(long id, String topic, int partition, long offset, K key, V value) {
        this.id = id;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

}
