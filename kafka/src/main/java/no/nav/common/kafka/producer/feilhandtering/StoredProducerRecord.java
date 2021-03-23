package no.nav.common.kafka.producer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class StoredProducerRecord {
    private final long id;
    private final String topic;
    private final byte[] key;
    private final byte[] value;
    private final String headersJson;

    public StoredProducerRecord(String topic, byte[] key, byte[] value, String headersJson) {
        this.id = -1;
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.headersJson = headersJson;
    }
}
