package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Should implement interface for easier mocking
public class KafkaProducerClient {

    // TODO: Mulig at vi må ha støtte for at man skal kunne spesifisere annet enn <String, String>
    private final Producer<String, String> producer;

    private final AtomicBoolean isShutDown;

    public KafkaProducerClient(KafkaProducerClientConfig config) {
        producer = new KafkaProducer<>(config.properties);

        isShutDown = new AtomicBoolean();

        registerShutdownHook();
    }

    public void sendSync(final ProducerRecord<String, String> record) throws ExecutionException, InterruptedException, TimeoutException {
        checkIfShutDown();

        try {
            producer.send(record).get(1000L, TimeUnit.MILLISECONDS); // Blocks for max 1 second or fails
            // TODO: Invoke listener onSuccess
        } catch (Exception e) {
            // TODO: Invoke listener onError
        }
    }

    public void sendAsync(final ProducerRecord<String, String> record, Callback callback) {
        checkIfShutDown();

        producer.send(record, callback);
    }

    public Producer<String, String> getProducer() {
        return producer;
    }

    private void checkIfShutDown() {
        if (isShutDown.get()) {
            throw new IllegalStateException("Cannot send messages to kafka after shutdown");
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShutDown.set(true);
            producer.close(Duration.ofSeconds(10));
        }));
    }

}
