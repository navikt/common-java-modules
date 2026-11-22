package no.nav.common.kafka.producer.util;

import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE;
import static no.nav.common.kafka.producer.util.KafkaProducerClientWithMetrics.KAFKA_PRODUCER_STATUS_COUNTER;
import static no.nav.common.kafka.utils.TestUtils.KAFKA_IMAGE;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestProducerProperties;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
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
    private MeterRegistry registry;
    private KafkaProducerClientWithMetrics<String, String> metricsClient;

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
    public void setup() throws Exception {
        String brokerUrl = kafka.getBootstrapServers();

        try (AdminClient admin =
                KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl))) {
            var eksisterendeTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);
            List<String> topicsSomSkalSlettes = Stream.of(TEST_TOPIC_A, TEST_TOPIC_B)
                    .filter(eksisterendeTopics::contains)
                    .toList();
            if (!topicsSomSkalSlettes.isEmpty()) {
                admin.deleteTopics(topicsSomSkalSlettes).all().get(10, TimeUnit.SECONDS);
            }
            admin.createTopics(
                            List.of(new NewTopic(TEST_TOPIC_A, 1, (short) 1), new NewTopic(TEST_TOPIC_B, 1, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }
        registry = new SimpleMeterRegistry();
        metricsClient = new KafkaProducerClientWithMetrics<>(
                kafkaTestProducerProperties(kafka.getBootstrapServers()), registry);
    }

    @After
    public void cleanup() {
        try {
            if (metricsClient != null) {
                metricsClient.close();
            }
        } finally {
            if (registry != null) {
                registry.close();
            }
        }
    }

    @Test
    public void registrererStatusTellerForVellykkedeSendinger() {
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(3, getStatusCount(registry, TEST_TOPIC_A, false), 0);
            assertEquals(2, getStatusCount(registry, TEST_TOPIC_B, false), 0);
        });
    }

    @Test
    public void registrererStatusTellerForFeiledeSendinger() {
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
    public void registrererGjeldendeOffsetPerTopic() {
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_A, "value"));

        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));
        metricsClient.send(new ProducerRecord<>(TEST_TOPIC_B, "value"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Gauge gaugeA = registry.find(KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE)
                    .tags("topic", TEST_TOPIC_A, "partition", "0")
                    .gauge();
            Gauge gaugeB = registry.find(KAFKA_PRODUCER_CURRENT_OFFSET_GAUGE)
                    .tags("topic", TEST_TOPIC_B, "partition", "0")
                    .gauge();
            assertNotNull("Offset-gauge mangler for topic A", gaugeA);
            assertNotNull("Offset-gauge mangler for topic B", gaugeB);
            assertEquals(2, gaugeA.value(), 0);
            assertEquals(1, gaugeB.value(), 0);
        });
    }

    private double getStatusCount(MeterRegistry registry, String topic, boolean failed) {
        return registry.counter(KAFKA_PRODUCER_STATUS_COUNTER, "topic", topic, "status", failed ? "failed" : "ok")
                .count();
    }
}
