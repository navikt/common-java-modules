package no.nav.common.kafka.producer.feilhandtering;

import java.time.Instant;
import java.util.List;

public interface KafkaProducerRepository {

    long storeRecord(StoredProducerRecord record);

    void deleteRecord(long id);

    List<StoredProducerRecord> getRecords(Instant olderThan, int maxMessages);

}
