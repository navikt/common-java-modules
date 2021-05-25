package no.nav.common.kafka.producer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.AuthenticationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Kafka producer client that can be used when credentials for Kafka authentication is changed during runtime.
 * {@link RollingCredentialsKafkaProducerClient} will create a new {@link KafkaProducerClient} whenever a {@link AuthenticationException}
 * is thrown. The action will be retried with the new client once, but if this also fails then the exception will be passed along.
 * @param <K> topic key
 * @param <V> topic value
 */
@Slf4j
public class RollingCredentialsKafkaProducerClient<K, V> implements KafkaProducerClient<K, V> {

    private final Supplier<KafkaProducerClient<K, V>> producerClientSupplier;

    private volatile KafkaProducerClient<K, V> kafkaProducerClient;

    public RollingCredentialsKafkaProducerClient(Supplier<KafkaProducerClient<K, V>> producerClientSupplier) {
        this.producerClientSupplier = producerClientSupplier;
        kafkaProducerClient = producerClientSupplier.get();
    }

    @Override
    public void close() {
        kafkaProducerClient.close();
    }

    @SneakyThrows
    @Override
    public RecordMetadata sendSync(ProducerRecord<K, V> record) {
        try {
            return send(record, null).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        Callback errorHandlerCallback = (RecordMetadata metadata, Exception exception) -> {
            try {
                if (exception instanceof AuthenticationException) {
                    log.info("Received AuthenticationException when sending record. Recreating producer client");
                    kafkaProducerClient = producerClientSupplier.get();
                    kafkaProducerClient.send(record, callback);
                } else if (callback != null) {
                    callback.onCompletion(metadata, exception);
                }
            } catch (Exception e) {
                log.error("Unexpected exception caught when recreating client due to AuthenticationException", e);
            }
        };

        return kafkaProducerClient.send(record, errorHandlerCallback);
    }

    @Override
    public Producer<K, V> getProducer() {
        return kafkaProducerClient.getProducer();
    }

}
