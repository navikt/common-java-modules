package no.nav.common.kafka.consumer.feilhandtering;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@AllArgsConstructor
@Data
public class StoredConsumerRecord {
    private final long id;
    private final String topic;
    private final int partition;
    private final long offset;
    private final byte[] key;
    private final byte[] value;
    private final String headersJson;
    private final int retries;
    private final Timestamp lastRetry;
    private final long timestamp;

    public StoredConsumerRecord(String topic, int partition, long offset, byte[] key, byte[] value, String headersJson, long timestamp) {
        this.id = -1;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
        this.headersJson = headersJson;
        this.timestamp = timestamp;
        this.retries = 0;
        this.lastRetry = null;
    }
}
