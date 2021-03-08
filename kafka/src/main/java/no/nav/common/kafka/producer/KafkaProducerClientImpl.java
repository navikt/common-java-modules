package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaProducerClientImpl<K, V> implements KafkaProducerClient<K, V> {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerClientImpl.class);

    private volatile boolean isClosed = false;

    private final Producer<K, V> producer;

    public KafkaProducerClientImpl(Producer<K, V> producer) {
        this.producer = producer;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public KafkaProducerClientImpl(Properties properties) {
        this(new KafkaProducer<>(properties));
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        log.info("Closing kafka producer...");

        try {
            producer.close(Duration.ofSeconds(5));
            log.info("Kafke producer was closed successfully");
        } catch (Exception e) {
            log.error("Failed to close kafka producer gracefully", e);
        }
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
       return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(final ProducerRecord<K, V> record, Callback callback) {
        if (isClosed) {
            throw new IllegalStateException("Cannot send messages to kafka after shutdown");
        }

        return producer.send(record, callback);
    }

}
