package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.Counter;
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

    private final static String COUNTER_NAME = "kafka.consumer.status";

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> counterMap = new HashMap<>();

    public TopicConsumerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onConsumed(ConsumerRecord<K, V> record, ConsumeStatus status) {
        String key = String.format("%s-%d-%s", record.topic(), record.partition(), status);

        counterMap.computeIfAbsent(key, (k) ->
                Counter.builder(COUNTER_NAME)
                    .tag("topic", record.topic())
                    .tag("partition", String.valueOf(record.partition()))
                    .tag("status", status.name())
                    .register(meterRegistry))
                .increment();
    }

}
