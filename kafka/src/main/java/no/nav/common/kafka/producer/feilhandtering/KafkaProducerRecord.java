package no.nav.common.kafka.producer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class KafkaProducerRecord {
    private final long id;
    private final String topic;
    private final byte[] key;
    private final byte[] value;

    public KafkaProducerRecord(String topic, byte[] key, byte[] value) {
        this.id = -1;
        this.topic = topic;
        this.key = key;
        this.value = value;
    }
}
