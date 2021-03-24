package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaProducerClientWithMetrics<K, V> implements KafkaProducerClient<K, V> {

    public final static String KAFKA_PRODUCER_STATUS_COUNTER = "kafka_producer_status";

    public final static String KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE = "kafka_producer_current_offset";

    private final KafkaProducerClient<K, V> client;

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> statusCounterMap = new HashMap<>();

    private final Map<String, Gauge> currentOffsetGaugeMap = new HashMap<>();

    private final Map<String, Long> currentOffsetMap = new HashMap<>();

    public KafkaProducerClientWithMetrics(Properties properties, MeterRegistry meterRegistry) {
        this.client = new KafkaProducerClientImpl<>(properties);
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
    public RecordMetadata sendSync(ProducerRecord<K, V> record) {
        try {
            RecordMetadata recordMetadata = client.sendSync(record);
            updateLatestOffset(recordMetadata);
            incrementRecordCount(record, false);
            return recordMetadata;
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

            if (metadata != null) {
                updateLatestOffset(metadata);
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        };

        return client.send(record, metricCallback);
    }

    @Override
    public Producer<K, V> getProducer() {
        return client.getProducer();
    }

    private void updateLatestOffset(RecordMetadata metadata) {
        String key = metadata.topic() + "-" + metadata.partition();

        long currentOffset = metadata.hasOffset()
                ? metadata.offset()
                : 0;

        currentOffsetMap.put(key, currentOffset);

        currentOffsetGaugeMap.computeIfAbsent(key, (k) ->
                Gauge.builder(KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE, () -> {
                    Long offset = currentOffsetMap.get(key);
                    return offset != null ? offset : 0;
                })
                .tag("topic", metadata.topic())
                .tag("partition", String.valueOf(metadata.partition()))
                .register(meterRegistry));
    }

    private void incrementRecordCount(ProducerRecord<K, V> record, boolean failed) {
        String key = record.topic() + "-" + failed;
        statusCounterMap.computeIfAbsent(key, (k) ->
                Counter.builder(KAFKA_PRODUCER_STATUS_COUNTER)
                        .tag("topic", record.topic())
                        .tag("status", failed ? "failed" : "ok")
                        .register(meterRegistry))
                .increment();
    }

}
