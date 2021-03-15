package no.nav.common.kafka.producer;

import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class KafkaProducerClientImpl<K, V> implements KafkaProducerClient<K, V> {

    private final Logger log = LoggerFactory.getLogger(KafkaProducerClientImpl.class);

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
        log.info("Closing kafka producer...");

        try {
            producer.close(Duration.ofSeconds(5));
            log.info("Kafka producer was closed successfully");
        } catch (Exception e) {
            log.error("Failed to close kafka producer gracefully", e);
        }
    }

    @SneakyThrows
    @Override
    public RecordMetadata sendSync(ProducerRecord<K, V> record) {
        Future<RecordMetadata> future = send(record, null);
        producer.flush(); // This will block until all buffered records are sent
        return future.get();
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
       return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(final ProducerRecord<K, V> record, Callback callback) {
        try {
            return producer.send(record, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onCompletion(null, e);
            }
            return CompletableFuture.failedFuture(e);
        }
    }

}
