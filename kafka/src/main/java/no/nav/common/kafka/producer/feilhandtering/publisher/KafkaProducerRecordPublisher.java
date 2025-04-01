package no.nav.common.kafka.producer.feilhandtering.publisher;

import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord;

import java.io.Closeable;
import java.util.List;

public interface KafkaProducerRecordPublisher extends Closeable {

    /**
     * Publishes the stored records to Kafka and returns a list of IDs of records that were successfully published
     * and should be deleted from the database.
     */
    List<Long> publishStoredRecords(List<StoredProducerRecord> records);

}
