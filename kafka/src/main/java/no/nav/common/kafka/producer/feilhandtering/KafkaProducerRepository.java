package no.nav.common.kafka.producer.feilhandtering;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.List;

public interface KafkaProducerRepository<K, V> {

    long storeRecord(ProducerRecord<K, V> record);

    void deleteRecord(long id);

    List<KafkaProducerRecord<K, V>> getRecords(Instant olderThan, int maxMessages);

}
