package no.nav.common.kafka.consumer.feilhandtering;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;

public interface KafkaConsumerRepository {

    long storeRecord(StoredConsumerRecord record);

    void deleteRecords(List<Long> ids);

    boolean hasRecordWithKey(String topic, int partition, byte[] key);

    List<StoredConsumerRecord> getRecords(String topic, int partition, int maxRecords);

    void incrementRetries(long id);

    List<TopicPartition> getTopicPartitions(List<String> topics);

    Map<TopicPartition, Long> getFailedRecordCounts(List<String> topics);
}
