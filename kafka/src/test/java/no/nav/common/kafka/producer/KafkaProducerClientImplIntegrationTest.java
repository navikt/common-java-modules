package no.nav.common.kafka.producer;

import static no.nav.common.kafka.utils.TestUtils.KAFKA_IMAGE;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestConsumerProperties;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestProducerProperties;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.KafkaConsumerClientImpl;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaProducerClientImplIntegrationTest {

    public static ConfluentKafkaContainer kafka;
    private static String brokerUrl;
    private static AdminClient admin;

    private KafkaProducerClientImpl<String, String> producerClient;
    private String testTopic;

    @SneakyThrows
    @BeforeClass
    public static void setup() {
        kafka = new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafka.waitingFor(new HostPortWaitStrategy());
        kafka.start();

        brokerUrl = kafka.getBootstrapServers();
        admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
    }

    @AfterClass
    public static void tearDown() {
        admin.close();
        kafka.stop();
    }

    @Before
    public void beforeEach() {
        producerClient = new KafkaProducerClientImpl<>(kafkaTestProducerProperties(brokerUrl));
        testTopic = UUID.randomUUID().toString();
        admin.createTopics(List.of(new NewTopic(testTopic, 1, (short) 1)));
    }

    @After
    public void afterEach() {
        producerClient.close();
    }

    @Test
    public void should_produce_record() {
        // Arrange
        AtomicReference<ConsumerRecord<String, String>> recordRef = new AtomicReference<>();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(
                new KafkaConsumerClientConfig<>(kafkaTestConsumerProperties(brokerUrl), Map.of(testTopic, (record) -> {
                    recordRef.set(record);
                    return ConsumeStatus.OK;
                })));

        // Act
        consumerClient.start();
        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"));

        await().atMost(Duration.ofSeconds(10)).until(() -> recordRef.get() != null);
        consumerClient.stop();

        // Assert
        ConsumerRecord<String, String> record = recordRef.get();
        assertEquals(testTopic, record.topic());
        assertEquals("key", record.key());
        assertEquals("value", record.value());
    }

    @Test
    public void should_produce_multiple_records() {
        // Arrange
        var counter = new AtomicInteger();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(
                new KafkaConsumerClientConfig<>(kafkaTestConsumerProperties(brokerUrl), Map.of(testTopic, (record) -> {
                    counter.incrementAndGet();
                    return ConsumeStatus.OK;
                })));

        // Act
        consumerClient.start();

        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"));
        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"));
        producerClient.send(new ProducerRecord<>(testTopic, "key", null));
        producerClient.send(new ProducerRecord<>(testTopic, null, "value"));
        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"));

        // Assert
        await().atMost(Duration.ofSeconds(10)).until(() -> counter.get() == 5);
        consumerClient.stop();
    }

    @Test
    public void should_produce_record_sync() {
        // Arrange
        var counter = new AtomicInteger();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(
                new KafkaConsumerClientConfig<>(kafkaTestConsumerProperties(brokerUrl), Map.of(testTopic, (record) -> {
                    counter.incrementAndGet();
                    return ConsumeStatus.OK;
                })));

        // Act
        consumerClient.start();
        producerClient.sendSync(new ProducerRecord<>(testTopic, "key", "value"));

        // Assert
        await().atMost(Duration.ofSeconds(10)).until(() -> counter.get() == 1);

        consumerClient.stop();
    }

    @Test
    public void should_throw_exception_when_fail_to_send_sync() {
        // Arrange
        var producerRecord = new ProducerRecord<>("invalidTopicPartition", 999, "key", "value");

        // Act & Assert
        assertThrows(ExecutionException.class, () -> producerClient.sendSync(producerRecord));
    }

    @Test
    public void should_execute_callback_with_exception_when_fail_to_send() {

        // Arrange
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        // Act
        producerClient.send(
                new ProducerRecord<>("invalidTopicPartition", 999, "key", "value"),
                (metadata, exception) -> exceptionRef.set(exception));

        await().atMost(Duration.ofSeconds(10)).until(() -> exceptionRef.get() != null);

        // Assert
        assertEquals(TimeoutException.class, exceptionRef.get().getClass());
    }

    @Test
    public void should_execute_callback_with_exception_when_sending_after_closed() {
        // Arrange
        var counter = new AtomicInteger();

        Callback callback = (metadata, exception) -> {
            if (exception != null) {
                counter.incrementAndGet();
            }
        };

        producerClient.close();

        // Act
        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"), callback);
        producerClient.send(new ProducerRecord<>(testTopic, "key", "value"), callback);

        // Assert
        assertEquals(2, counter.get());
    }

    @Test
    public void should_throw_exception_when_sending_after_closed() {
        // Arrange
        producerClient.close();

        // Act & Assert
        assertThrows(
                ExecutionException.class,
                () -> producerClient.sendSync(new ProducerRecord<>(testTopic, "key", "value")));
    }
}
