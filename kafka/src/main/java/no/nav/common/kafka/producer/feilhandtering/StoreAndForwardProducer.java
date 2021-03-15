package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

public class StoreAndForwardProducer<K, V> implements KafkaProducerClient<K, V> {

    private final Logger log = LoggerFactory.getLogger(StoreAndForwardProducer.class);

    private final KafkaProducerRepository<K, V> producerRepository;

    private final KafkaProducerClient<K, V> producerClient;

    public StoreAndForwardProducer(KafkaProducerClient<K, V> producerClient, KafkaProducerRepository<K, V> producerRepository) {
        this.producerClient = producerClient;
        this.producerRepository = producerRepository;
    }

    public StoreAndForwardProducer(Properties properties, KafkaProducerRepository<K, V> producerRepository) {
        this.producerClient = new KafkaProducerClientImpl<>(properties);
        this.producerRepository = producerRepository;
    }

    @Override
    public void close() {
        producerClient.close();
    }

    @Override
    public RecordMetadata sendSync(ProducerRecord<K, V> record) {
        return producerClient.sendSync(record);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        long id = producerRepository.storeRecord(record);

        return producerClient.send(record, (metadata, exception) -> {
            if (exception == null) {
                producerRepository.deleteRecord(id);
            } else {
                log.warn("Failed to send message. Message has been stored for retry", exception);
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

}
