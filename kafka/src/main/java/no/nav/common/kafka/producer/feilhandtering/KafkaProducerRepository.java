package no.nav.common.kafka.producer.feilhandtering;

import java.util.List;

public interface KafkaProducerRepository {

    long storeRecord(StoredProducerRecord record);

    void deleteRecords(List<Long> ids);

    List<StoredProducerRecord> getRecords(int maxMessages);

    List<StoredProducerRecord> getRecords(int maxMessages, List<String> topics);

}
