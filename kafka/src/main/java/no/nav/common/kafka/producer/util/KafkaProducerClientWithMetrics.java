package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaProducerClientWithMetrics<K, V> implements KafkaProducerClient<K, V> {

    private final static String KAFKA_PRODUCER_STATUS_COUNTER = "kafka.producer.status";

    private final KafkaProducerClient<K, V> client;

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> counterMap = new HashMap<>();

    public KafkaProducerClientWithMetrics(Properties properties, MeterRegistry meterRegistry) {
        this.client = new KafkaProducerClientImpl<K, V>(properties);
        this.meterRegistry = meterRegistry;
    }

    public KafkaProducerClientWithMetrics(KafkaProducerClient<K, V> client, MeterRegistry meterRegistry) {
        this.client = client;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void sendSync(ProducerRecord<K, V> record) {
        try {
            client.sendSync(record);
            incrementRecordCount(record, false);
        } catch (Exception e) {
            incrementRecordCount(record, true);
            throw e;
        }
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        return send(record, null);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        Callback metricCallback = (metadata, exception) -> {
            boolean failed = exception != null;

            incrementRecordCount(record, failed);

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        };

        return client.send(record, metricCallback);
    }

    private void incrementRecordCount(ProducerRecord<K, V> record, boolean failed) {
        String key = record.topic() + "-" + failed;
        counterMap.computeIfAbsent(key, (k) ->
                Counter.builder(KAFKA_PRODUCER_STATUS_COUNTER)
                    .tag("topic", record.topic())
                    .tag("status", failed ? "failed" : "ok")
                    .register(meterRegistry))
                .increment();
    }

}
