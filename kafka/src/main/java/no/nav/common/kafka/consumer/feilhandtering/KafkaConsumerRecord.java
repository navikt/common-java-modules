package no.nav.common.kafka.consumer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@AllArgsConstructor
@Data
public class KafkaConsumerRecord {
    private final long id;
    private final String topic;
    private final int partition;
    private final long offset;
    private final byte[] key;
    private final byte[] value;
    private final int retries;
    private final Timestamp lastRetry;

    public KafkaConsumerRecord(String topic, int partition, long offset, byte[] key, byte[] value) {
        this.id = -1;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
        this.retries = 0;
        this.lastRetry = null;
    }
}
