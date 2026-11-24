package no.nav.common.kafka.producer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
public class KafkaProducerClientImpl<K, V> implements KafkaProducerClient<K, V> {

    private final Producer<K, V> producer;

    public KafkaProducerClientImpl(Producer<K, V> producer) {
        this.producer = producer;
    }

    public KafkaProducerClientImpl(Properties properties) {
        this(properties, true);
    }

    /**
     * Oppretter en klient med valgfri JVM-shutdown-hook på produceren.
     *
     * @param properties           Kafka-konfigurasjon
     * @param registerShutdownHook true for automatisk lukking; ved false må applikasjonen kalle close
     */
    public KafkaProducerClientImpl(Properties properties, boolean registerShutdownHook) {
        this(new GracefulKafkaProducer<>(properties, registerShutdownHook));
    }

    @Override
    public void close() {
        log.info("Closing kafka producer...");
        producer.close();
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
        Callback callbackWithLog = wrapWithLog(callback);

        try {
            return producer.send(record, callbackWithLog);
        } catch (Exception e) {
            callbackWithLog.onCompletion(null, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Producer<K, V> getProducer() {
        return producer;
    }

    private Callback wrapWithLog(Callback callback) {
        return (metadata, exception) -> {
            if (metadata != null) {
                log.info(
                        "Record was sent topic={} partition={} offset={}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset());
            } else if (exception != null) {
                log.error("Failed to send record", exception);
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        };
    }
}
