package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Instant;
import java.util.List;

public interface KafkaConsumerRepository<K, V> {

    long storeRecord(ConsumerRecord<K, V> record);

    void deleteRecord(long id);

    List<KafkaConsumerRecord<K, V>> getRecords(List<String> topics, Instant olderThan, int maxMessages);

}
