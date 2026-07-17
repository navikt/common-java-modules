package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer listener which adds a consumption status metric for each topic + partition
 *
 * @param <K> topic key
 * @param <V> topic value
 */
public class TopicConsumerMetrics<K, V> implements TopicConsumerListener<K, V> {

    public final static String KAFKA_CONSUMER_STATUS_COUNTER = "kafka_consumer_status";

    public final static String KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE = "kafka_consumer_consumed_offset";

    private final MeterRegistry meterRegistry;

    private String consumerGroupId;

    private final Map<String, Counter> statusCounterMap = new ConcurrentHashMap<>();

    private final Map<String, Gauge> consumedOffsetGaugeMap = new ConcurrentHashMap<>();

    private final Map<String, Long> consumedOffsetMap = new ConcurrentHashMap<>();

    public TopicConsumerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    @Override
    public void onConsumed(ConsumerRecord<K, V> record, ConsumeStatus status) {
        String statusMapKey = String.format("%s-%d-%s-%s", record.topic(), record.partition(), status, consumerGroupId);

        statusCounterMap.computeIfAbsent(statusMapKey, (k) -> {
            Counter.Builder builder = Counter.builder(KAFKA_CONSUMER_STATUS_COUNTER)
                    .tag("topic", record.topic())
                    .tag("partition", String.valueOf(record.partition()))
                    .tag("status", status.name().toLowerCase());
            if (consumerGroupId != null) {
                builder.tag("group_id", consumerGroupId);
            }
            return builder.register(meterRegistry);
        }).increment();

        if (status == ConsumeStatus.OK) {
            String offsetMapKey = String.format("%s-%d-%s", record.topic(), record.partition(), consumerGroupId);

            consumedOffsetMap.put(offsetMapKey, record.offset());

            consumedOffsetGaugeMap.computeIfAbsent(offsetMapKey, (k) -> {
                var builder = Gauge.builder(KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE, () -> {
                            Long offset = consumedOffsetMap.get(offsetMapKey);
                            return offset != null ? offset : 0;
                        })
                        .description("The latest consumed offset. The offset is not guaranteed to have been committed.")
                        .tag("topic", record.topic())
                        .tag("partition", String.valueOf(record.partition()));
                if (consumerGroupId != null) {
                    builder.tag("group_id", consumerGroupId);
                }
                return builder.register(meterRegistry);
            });
        }
    }

}
