package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.util.ProducerUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaProducerRecordStorage {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerRecordStorage.class);

    private final KafkaProducerRepository producerRepository;

    public KafkaProducerRecordStorage(KafkaProducerRepository producerRepository) {
        this.producerRepository = producerRepository;
    }

    public void store(ProducerRecord<byte[], byte[]> record) {
        try {
            producerRepository.storeRecord(ProducerUtils.mapToStoredRecord(record));
            log.info("Stored producer record for topic " + record.topic());
        } catch (Exception e) {
            log.error("Failed to store producer record for topic " + record.topic(), e);
            throw e;
        }
    }

}
