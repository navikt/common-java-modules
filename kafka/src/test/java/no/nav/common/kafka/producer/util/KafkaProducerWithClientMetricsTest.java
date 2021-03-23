package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE;
import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_STATUS_COUNTER;
import static no.nav.common.kafka.utils.TestUtils.KAFKA_IMAGE;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestProducerProperties;
import static org.junit.Assert.assertEquals;

public class KafkaProducerWithClientMetricsTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @Before
    public void setup() {
        String brokerUrl = kafka.getBootstrapServers();

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        admin.deleteTopics(List.of(TEST_TOPIC_A, TEST_TOPIC_B));
        admin.createTopics(List.of(
                new NewTopic(TEST_TOPIC_A, 1, (short) 1),
                new NewTopic(TEST_TOPIC_B, 1, (short) 1)
                )
        );
        admin.close(); // Apply changes
    }

    @Test
    public void should_register_status_counter_ok_metric() throws InterruptedException {
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        Thread.sleep(1000);

        assertEquals(3, getStatusCount(registry, TEST_TOPIC_A, false), 0);
        assertEquals(2, getStatusCount(registry, TEST_TOPIC_B, false), 0);
    }

    @Test
    public void should_register_status_counter_failed_metric() {
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        metricsClient.close();

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        assertEquals(3, getStatusCount(registry, TEST_TOPIC_A, true), 0);
        assertEquals(2, getStatusCount(registry, TEST_TOPIC_B, true), 0);
    }

    @Test
    public void should_register_current_offset_metric() {
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        metricsClient.close();

        List<Gauge> gauges = new ArrayList<>(registry.get(KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE).gauges());

        assertEquals(2, gauges.get(0).value(), 0);
        assertEquals(1, gauges.get(1).value(), 0);
    }

    private double getStatusCount(MeterRegistry registry, String topic, boolean failed) {
        return registry.counter(
                KAFKA_PRODUCER_STATUS_COUNTER,
                "topic", topic,
                "status", failed ? "failed" : "ok"
        ).count();
    }

}
