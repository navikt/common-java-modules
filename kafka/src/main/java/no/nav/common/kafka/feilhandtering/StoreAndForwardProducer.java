package no.nav.common.kafka.feilhandtering;

import no.nav.common.kafka.feilhandtering.db.KafkaProducerRepository;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

public class StoreAndForwardProducer<K, V> {

    private final Logger log = LoggerFactory.getLogger(StoreAndForwardProducer.class);

    private final KafkaProducerRepository<K, V> producerRepository;

    private final KafkaProducerClient<K, V> producerClient;

    public StoreAndForwardProducer(KafkaProducerRepository<K, V> producerRepository, KafkaProducerClient<K, V> producerClient) {
        this.producerRepository = producerRepository;
        this.producerClient = producerClient;
    }

    public StoreAndForwardProducer(KafkaProducerRepository<K, V> producerRepository, Properties properties) {
        this.producerRepository = producerRepository;
        this.producerClient = new KafkaProducerClientImpl<>(properties);
    }

    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        long id = producerRepository.storeRecord(record);

        return producerClient.send(record, (metadata, exception) -> {
            if (exception == null) {
                producerRepository.deleteRecord(id);
            } else {
                log.warn("Failed to send message. Message has been stored for retry", exception);
            }
        });
    }

}
