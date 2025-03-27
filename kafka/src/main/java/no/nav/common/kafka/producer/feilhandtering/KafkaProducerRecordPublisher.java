package no.nav.common.kafka.producer.feilhandtering;

import java.io.Closeable;
import java.util.List;

public interface KafkaProducerRecordPublisher extends Closeable {

    /**
     * Publishes stored the records to Kafka and returns a list of IDs of records that were successfully published
     * and should be deleted from the database.
     */
    List<Long> publishStoredRecords(List<StoredProducerRecord> records);

}
