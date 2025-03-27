package no.nav.common.kafka.producer.feilhandtering;

import lombok.SneakyThrows;
import no.nav.common.kafka.producer.GracefulKafkaProducer;
import no.nav.common.kafka.producer.KafkaProducerClient;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;


class TestKafkaProducerClient implements KafkaProducerClient<byte[], byte[]> {

    private final Producer<byte[], byte[]> producer;
    private Consumer<ProducerRecord<byte[], byte[]>> onSend;

    public TestKafkaProducerClient(Properties properties) {
        this.producer = new GracefulKafkaProducer<>(properties);
    }

    public void setOnSend(Consumer<ProducerRecord<byte[], byte[]>> onSend) {
        this.onSend = onSend;
    }

    @Override
    public void close() {
        producer.close();
    }

    @SneakyThrows
    @Override
    public RecordMetadata sendSync(ProducerRecord<byte[], byte[]> record) {
        Future<RecordMetadata> future = send(record, null);
        producer.flush();
        return future.get();
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<byte[], byte[]> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(final ProducerRecord<byte[], byte[]> record, Callback callback) {
        try {
            if (this.onSend != null) {
                this.onSend.accept(record);
            }
            return producer.send(record, callback);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Producer<byte[], byte[]> getProducer() {
        return producer;
    }
}
