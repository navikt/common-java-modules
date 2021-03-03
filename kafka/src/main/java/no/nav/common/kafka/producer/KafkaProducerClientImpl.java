package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.*;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaProducerClientImpl<K, V> implements KafkaProducerClient<K, V> {

    private final Producer<K, V> producer;
    private final AtomicBoolean isShutDown = new AtomicBoolean();

    public KafkaProducerClientImpl(Properties properties) {
        producer = new KafkaProducer<>(properties);
        registerShutdownHook();
    }

    @Override
    public Future<RecordMetadata> send(final ProducerRecord<K, V> record, Callback callback) {
        checkIfShutDown();
        return producer.send(record, callback);
    }

    private void checkIfShutDown() {
        if (isShutDown.get()) {
            throw new IllegalStateException("Cannot send messages to kafka after shutdown");
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShutDown.set(true);
            producer.close(Duration.ofSeconds(5));
        }));
    }

}
