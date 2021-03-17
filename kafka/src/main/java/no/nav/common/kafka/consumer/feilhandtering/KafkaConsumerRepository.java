package no.nav.common.kafka.consumer.feilhandtering;

import org.apache.kafka.common.TopicPartition;

import java.util.List;

public interface KafkaConsumerRepository {

    long storeRecord(KafkaConsumerRecord record);

    void deleteRecords(List<Long> ids);

    boolean hasRecordWithKey(String topic, int partition, byte[] key);

    List<KafkaConsumerRecord> getRecords(String topic, int partition, int maxRecords);

    void incrementRetries(long id);

    List<TopicPartition> getTopicPartitions(List<String> topics);

}
