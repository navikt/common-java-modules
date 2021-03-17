package no.nav.common.kafka.producer.feilhandtering;

import java.time.Instant;
import java.util.List;

public interface KafkaProducerRepository {

    long storeRecord(KafkaProducerRecord record);

    void deleteRecord(long id);

    List<KafkaProducerRecord> getRecords(Instant olderThan, int maxMessages);

}
