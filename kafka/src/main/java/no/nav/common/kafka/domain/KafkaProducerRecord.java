package no.nav.common.kafka.domain;

public class KafkaProducerRecord<K, V> {
    public final long id;
    public final String topic;
    public final K key;
    public final V value;

    public KafkaProducerRecord(long id, String topic, K key, V value) {
        this.id = id;
        this.topic = topic;
        this.key = key;
        this.value = value;
    }
}
