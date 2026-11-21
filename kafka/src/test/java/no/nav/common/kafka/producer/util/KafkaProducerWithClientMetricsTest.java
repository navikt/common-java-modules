package no.nav.common.kafka.producer.util;

import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE;
import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_STATUS_COUNTER;
import static no.nav.common.kafka.utils.TestUtils.KAFKA_IMAGE;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestProducerProperties;
import static org.junit.Assert.assertEquals;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaProducerWithClientMetricsTest {
    private static final String TEST_TOPIC_A = "test-topic-a";
    private static final String TEST_TOPIC_B = "test-topic-b";

    private static ConfluentKafkaContainer kafka;

    @BeforeClass
    public static void beforeClass() {
        kafka = new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafka.waitingFor(new HostPortWaitStrategy());
        kafka.start();
    }

    @AfterClass
    public static void afterClass() {
        kafka.stop();
    }

    @Before
    public void setup() {
        String brokerUrl = kafka.getBootstrapServers();

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        admin.deleteTopics(List.of(TEST_TOPIC_A, TEST_TOPIC_B));
        admin.createTopics(List.of(new NewTopic(TEST_TOPIC_A, 1, (short) 1), new NewTopic(TEST_TOPIC_B, 1, (short) 1)));
        admin.close(); // Apply changes
    }

    @Test
    public void should_register_status_counter_ok_metric() throws InterruptedException {
        // Arrange
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(
            kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        // Act
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        Thread.sleep(1000);

        // Assert
        assertEquals(3, getStatusCount(registry, TEST_TOPIC_A, false), 0);
        assertEquals(2, getStatusCount(registry, TEST_TOPIC_B, false), 0);
    }

    @Test
    public void should_register_status_counter_failed_metric() {
        // Arrange
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(
            kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        metricsClient.close();

        // Act
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        // Assert
        assertEquals(3, getStatusCount(registry, TEST_TOPIC_A, true), 0);
        assertEquals(2, getStatusCount(registry, TEST_TOPIC_B, true), 0);
    }

    @Test
    public void should_register_current_offset_metric() {
        // Arrange
        MeterRegistry registry = new SimpleMeterRegistry();

        KafkaProducerClientWithMetrics<String, String> metricsClient = new KafkaProducerClientWithMetrics<>(
            kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);

        // Act
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        metricsClient.close();

        // Assert
        List<Gauge> gauges = new ArrayList<>(
            registry.get(KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE).gauges());

        assertEquals(2, gauges.get(0).value(), 0);
        assertEquals(1, gauges.get(1).value(), 0);
    }

    private double getStatusCount(MeterRegistry registry, String topic, boolean failed) {
        return registry.counter(KAFKA_PRODUCER_STATUS_COUNTER, "topic", topic, "status", failed ? "failed" : "ok")
            .count();
    }
}
