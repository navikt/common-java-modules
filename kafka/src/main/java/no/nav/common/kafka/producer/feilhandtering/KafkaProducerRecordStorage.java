package no.nav.common.kafka.producer.feilhandtering;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaProducerRecordStorage<K, V> {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerRecordStorage.class);

    private final KafkaProducerRepository<K, V> producerRepository;

    public KafkaProducerRecordStorage(KafkaProducerRepository<K, V> producerRepository) {
        this.producerRepository = producerRepository;
    }

    public void store(ProducerRecord<K, V> record) {
        try {
            producerRepository.storeRecord(record);
            log.info("Stored record for topic " + record.topic());
        } catch (Exception e) {
            log.error("Failed to store record for topic " + record.topic(), e);
            throw e;
        }
    }

}
