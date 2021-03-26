package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.util.ProducerUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaProducerRecordStorage<K, V> {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerRecordStorage.class);

    private final KafkaProducerRepository producerRepository;

    private final Serializer<K> keySerializer;

    private final Serializer<V> valueSerializer;

    public KafkaProducerRecordStorage(
            KafkaProducerRepository producerRepository,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer
    ) {
        this.producerRepository = producerRepository;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    public void store(ProducerRecord<K, V> record) {
        try {
            producerRepository.storeRecord(ProducerUtils.mapToStoredRecord(record, keySerializer, valueSerializer));
            log.info("Stored producer record for topic " + record.topic());
        } catch (Exception e) {
            log.error("Failed to store producer record for topic " + record.topic(), e);
            throw e;
        }
    }

}
