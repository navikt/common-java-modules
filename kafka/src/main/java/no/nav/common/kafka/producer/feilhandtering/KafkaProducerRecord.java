package no.nav.common.kafka.producer.feilhandtering;

import lombok.Data;

@Data
public class KafkaProducerRecord<K, V> {
    private final long id;
    private final String topic;
    private final K key;
    private final V value;

    public KafkaProducerRecord(long id, String topic, K key, V value) {
        this.id = id;
        this.topic = topic;
        this.key = key;
        this.value = value;
    }
}
