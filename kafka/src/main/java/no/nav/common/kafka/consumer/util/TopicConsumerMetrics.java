package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer listener which adds a consumption status metric for each topic + partition
 * @param <K> topic key
 * @param <V> topic value
 */
public class TopicConsumerMetrics<K, V> implements TopicConsumerListener<K, V> {

    public final static String KAFKA_CONSUMER_STATUS_COUNTER = "kafka.consumer.status";

    public final static String KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE = "kafka.consumer.consumed-offset";

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> statusCounterMap = new HashMap<>();

    private final Map<String, Gauge> consumedOffsetGaugeMap = new HashMap<>();

    private final Map<String, Long> consumedOffsetMap = new HashMap<>();

    public TopicConsumerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onConsumed(ConsumerRecord<K, V> record, ConsumeStatus status) {
        String statusMapKey = String.format("%s-%d-%s", record.topic(), record.partition(), status);

        statusCounterMap.computeIfAbsent(statusMapKey, (k) ->
                Counter.builder(KAFKA_CONSUMER_STATUS_COUNTER)
                        .tag("topic", record.topic())
                        .tag("partition", String.valueOf(record.partition()))
                        .tag("status", status.name().toLowerCase())
                        .register(meterRegistry))
                .increment();

        if (status == ConsumeStatus.OK) {
            String offsetMapKey = String.format("%s-%d", record.topic(), record.partition());

            consumedOffsetMap.put(offsetMapKey, record.offset());

            consumedOffsetGaugeMap.computeIfAbsent(statusMapKey, (k) ->
                    Gauge.builder(KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE, () -> {
                        Long offset = consumedOffsetMap.get(offsetMapKey);
                        return offset != null ? offset : 0;
                    })
                    .description("The latest consumed offset. The offset is not guaranteed to have been committed.")
                    .tag("topic", record.topic())
                    .tag("partition", String.valueOf(record.partition()))
                    .register(meterRegistry));
        }
    }

}
