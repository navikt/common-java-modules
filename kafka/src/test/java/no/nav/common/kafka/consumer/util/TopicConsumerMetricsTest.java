package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static no.nav.common.kafka.consumer.util.TopicConsumerMetrics.KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE;
import static no.nav.common.kafka.consumer.util.TopicConsumerMetrics.KAFKA_CONSUMER_STATUS_COUNTER;
import static org.junit.Assert.assertEquals;

public class TopicConsumerMetricsTest {

    @Test
    public void should_report_status_metrics_when_records_are_consumed() {
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


    @Test
    public void should_report_offset_metrics_when_records_are_consumed() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TopicConsumerMetrics<String, String> topicConsumerMetrics = new TopicConsumerMetrics<>(registry);

        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 1L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 2L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-1", 1, 3L, "", ""), ConsumeStatus.OK);

        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-2", 1, 3L, "", ""), ConsumeStatus.OK);
        topicConsumerMetrics.onConsumed(new ConsumerRecord<>("topic-2", 2, 4L, "", ""), ConsumeStatus.OK);

        List<Gauge> gauges = new ArrayList<>(registry.get(KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE).gauges());

        assertEquals(3, gauges.get(0).value(), 0);
        assertEquals(3, gauges.get(1).value(), 0);
        assertEquals(4, gauges.get(2).value(), 0);
    }

    @Test
    public void should_not_produce_duplicate_metrics_when_two_consumers_share_the_same_topic() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TopicConsumerMetrics<String, String> consumer1 = new TopicConsumerMetrics<>(registry);
        consumer1.setConsumerGroupId("group-1");
        TopicConsumerMetrics<String, String> consumer2 = new TopicConsumerMetrics<>(registry);
        consumer2.setConsumerGroupId("group-2");

        consumer1.onConsumed(new ConsumerRecord<>("shared-topic", 1, 1L, "", ""), ConsumeStatus.OK);
        consumer2.onConsumed(new ConsumerRecord<>("shared-topic", 1, 2L, "", ""), ConsumeStatus.OK);

        assertEquals(1, getCountWithGroupId(registry, "shared-topic", 1, ConsumeStatus.OK, "group-1"), 0);
        assertEquals(1, getCountWithGroupId(registry, "shared-topic", 1, ConsumeStatus.OK, "group-2"), 0);

        List<Gauge> gauges = new ArrayList<>(registry.get(KAFKA_CONSUMER_CONSUMED_OFFSET_GAUGE).gauges());
        assertEquals(2, gauges.size());
        assertEquals(1, gauges.stream().filter(g -> "group-1".equals(g.getId().getTag("group_id"))).findFirst().orElseThrow().value(), 0);
        assertEquals(2, gauges.stream().filter(g -> "group-2".equals(g.getId().getTag("group_id"))).findFirst().orElseThrow().value(), 0);
    }

    private double getCount(MeterRegistry registry, String topic, int partition, ConsumeStatus status) {
        return registry.counter(
                KAFKA_CONSUMER_STATUS_COUNTER,
                "topic", topic,
                "partition", String.valueOf(partition),
                "status", status.name().toLowerCase()
        ).count();
    }

    private double getCountWithGroupId(MeterRegistry registry, String topic, int partition, ConsumeStatus status, String groupId) {
        return registry.counter(
                KAFKA_CONSUMER_STATUS_COUNTER,
                "topic", topic,
                "partition", String.valueOf(partition),
                "status", status.name().toLowerCase(),
                "group_id", groupId
        ).count();
    }

}
