package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import static no.nav.common.kafka.consumer.util.TopicConsumerMetrics.KAFKA_CONSUMER_STATUS_COUNTER;
import static org.junit.Assert.assertEquals;

public class TopicConsumerMetricsTest {

    @Test
    public void should_report_metrics_when_records_are_consumed() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TopicConsumerMetrics<String, String> topicConsumerMetrics = new TopicConsumerMetrics<>(registry);

        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 1L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 2L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 3L, "", ""), ConsumeStatus.FAILED);

        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-2", 1, 3L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-2", 2, 3L, "", ""), ConsumeStatus.FAILED);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-2", 2, 3L, "", ""), ConsumeStatus.FAILED);

        assertEquals(2, getCount(registry, "topic-1", 1, ConsumeStatus.OK), 0);
        assertEquals(1, getCount(registry, "topic-1", 1, ConsumeStatus.FAILED), 0);

        assertEquals(1, getCount(registry, "topic-2", 1, ConsumeStatus.OK), 0);
        assertEquals(2, getCount(registry, "topic-2", 2, ConsumeStatus.FAILED), 0);
    }

    private double getCount(MeterRegistry registry, String topic, int partition, ConsumeStatus status) {
        return registry.counter(
                KAFKA_CONSUMER_STATUS_COUNTER,
                "topic", topic,
                "partition", String.valueOf(partition),
                "status", status.name().toLowerCase()
        ).count();
    }

}
