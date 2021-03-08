package no.nav.common.kafka.feilhandtering;

import no.nav.common.kafka.feilhandtering.db.KafkaRepository;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

public class StoreAndForwardProducer implements KafkaProducerClient<String, String> {

    private final Logger log = LoggerFactory.getLogger(StoreAndForwardProducer.class);

    private final KafkaRepository repository;

    private final KafkaProducerClient<String, String> client;

    public StoreAndForwardProducer(KafkaRepository repository, KafkaProducerClient<String, String> client) {
        this.repository = repository;
        this.client = client;
    }

    public StoreAndForwardProducer(KafkaRepository repository, Properties properties) {
        this.repository = repository;
        this.client = new KafkaProducerClientImpl<>(properties);
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, String> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, String> record, Callback callback) {
        long id = repository.storeProducerMessage(record.topic(), record.key(), record.value());

        return client.send(record, (metadata, exception) -> {
            if (exception == null) {
                repository.deleteProducerMessage(id);
            } else {
                log.warn("Failed to send message. Message has been stored for retry", exception);
                repository.failed(id);
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

}
