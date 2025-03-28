package no.nav.common.kafka.producer.feilhandtering.publisher;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord;
import no.nav.common.kafka.producer.util.ProducerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class QueuedKafkaProducerRecordPublisher implements KafkaProducerRecordPublisher {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final KafkaProducerClient<byte[], byte[]> producerClient;

    public QueuedKafkaProducerRecordPublisher(KafkaProducerClient<byte[], byte[]> producerClient) {
        this.producerClient = producerClient;
    }

    public List<Long> publishStoredRecords(List<StoredProducerRecord> records) {
        List<Long> idsToDelete = new ArrayList<>();

        for (StoredProducerRecord record : records) {
            try {
                producerClient.sendSync(ProducerUtils.mapFromStoredRecord(record));
                idsToDelete.add(record.getId());
            } catch (Exception e) {
                log.warn("Failed to send record to topic {}", record.getTopic(), e);
                break;
            }
        }

        return idsToDelete;
    }

    @Override
    public void close() {
        producerClient.close();
    }
}
