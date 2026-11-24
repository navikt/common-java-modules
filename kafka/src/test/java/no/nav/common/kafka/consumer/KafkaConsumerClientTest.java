package no.nav.common.kafka.consumer;

import static no.nav.common.kafka.utils.TestUtils.KAFKA_IMAGE;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestConsumerProperties;
import static no.nav.common.kafka.utils.TestUtils.kafkaTestProducerProperties;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaConsumerClientTest {
    private String testTopicA;
    private String testTopicB;

    private String consumerGroupId;
    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> commitChecker;
    private KafkaConsumerClient consumerClient;
    private KafkaConsumerClient consumerClient2;
    private static ConfluentKafkaContainer kafka;

    @BeforeAll
    public static void beforeClass() {
        kafka = new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafka.waitingFor(new HostPortWaitStrategy());
        kafka.start();
    }

    @AfterAll
    public static void afterClass() {
        kafka.stop();
    }

    @BeforeEach
    public void setup() {
        consumerGroupId = UUID.randomUUID().toString();
        String brokerUrl = kafka.getBootstrapServers();
        producer = new KafkaProducer<>(kafkaTestProducerProperties(brokerUrl));
        commitChecker = new KafkaConsumer<>(kafkaTestConsumerProperties(brokerUrl, consumerGroupId));

        try (AdminClient ignored =
                KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl))) {
            testTopicA = UUID.randomUUID().toString();
            testTopicB = UUID.randomUUID().toString();
        }
    }

    @AfterEach
    public void cleanup() {
        try {
            if (consumerClient2 != null) {
                consumerClient2.stop();
            }
        } finally {
            try {
                if (consumerClient != null) {
                    consumerClient.stop();
                }
            } finally {
                try {
                    if (commitChecker != null) {
                        commitChecker.close();
                    }
                } finally {
                    if (producer != null) {
                        producer.close();
                    }
                }
            }
        }
    }

    @Test
    public void should_consume_and_commit_offsets_and_start_consuming_again_on_excepted_offset() {
        // Arrange
        var counter = new AtomicInteger();

        var config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter, 0)));

        producer.send(new ProducerRecord<>(testTopicA, "key1", "value1"));
        producer.send(new ProducerRecord<>(testTopicA, "key1", "value2"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", "value3"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", null));
        producer.send(new ProducerRecord<>(testTopicA, null, "value5"));

        producer.flush();

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));

        // Act
        consumerClient.stop();

        // Assert
        assertEquals(5, counter.get());

        // Arrange
        producer.send(new ProducerRecord<>(testTopicA, "key1", "value4"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", "value5"));

        producer.flush();

        // Act
        consumerClient.start();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 7, 0));

        // Act
        consumerClient.stop();

        // Assert
        assertEquals(7, counter.get());
    }

    @Test
    public void should_commit_consumed_tasks_when_closed_gracefully() {
        // Arrange
        var counter = new AtomicInteger();

        for (int i = 0; i < 15; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }

        producer.flush();

        // Slow consumer (100ms wait)
        var config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter, 100)),
                10);

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();
        await().until(() -> counter.get() >= 4);

        // Stop client after approx 4 messages
        consumerClient.stop();

        // Assert
        OffsetAndMetadata committedOffsets = getCommittedOffsets(testTopicA, 0);

        assertTrue(committedOffsets.offset() > 4);
        assertTrue(committedOffsets.offset() < 12);
        assertEquals(counter.get(), committedOffsets.offset());
    }

    @Test
    public void should_consume_from_several_partitions() {
        // Arrange
        String multiPartitionTopic = "multi-partition-topic";

        try (AdminClient admin = KafkaAdminClient.create(
                Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(multiPartitionTopic, 2, (short) 1)));
        }

        var counter = new AtomicInteger();

        var config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(multiPartitionTopic, consumerWithCounterAndSleep(counter, 0)));

        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value3"));

        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value3"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value4"));

        producer.flush();

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();

        // Assert
        await().atMost(Duration.ofSeconds(5))
                .until(() -> assertCommittedOffsetEquals(multiPartitionTopic, 3, 0)
                        && assertCommittedOffsetEquals(multiPartitionTopic, 4, 1));

        await().atMost(Duration.ofSeconds(5)).until(() -> counter.get() == 7);
        consumerClient.stop();
    }

    @Test
    public void should_stop_consumption_of_topic_on_failure() {
        // Arrange
        var messagesReadCounterA = new AtomicInteger();
        var messagesReadCounterB = new AtomicInteger();
        var maxRecordOffsetB = new AtomicLong(-1);
        TopicConsumer<String, String> failOnOffsetConsumer = consumerRecord -> {
            messagesReadCounterB.incrementAndGet();
            maxRecordOffsetB.set(Math.max(maxRecordOffsetB.get(), consumerRecord.offset()));
            return consumerRecord.offset() == 2 ? ConsumeStatus.FAILED : ConsumeStatus.OK;
        };

        var config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(
                        testTopicA,
                        consumerWithCounterAndSleep(messagesReadCounterA, 0),
                        testTopicB,
                        failOnOffsetConsumer));

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
            producer.send(new ProducerRecord<>(testTopicB, "key1", "value" + i));
        }

        producer.flush();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicB, 2, 0));

        await().atMost(Duration.ofSeconds(5)).until(() -> messagesReadCounterA.get() == 5);
        await().atMost(Duration.ofSeconds(5)).until(() -> maxRecordOffsetB.get() == 2);
        assertTrue(messagesReadCounterB.get() >= 3, "Should have been reading messages");

        consumerClient.stop();
    }

    @Test
    public void should_stop_consumption_of_topic_on_and_seek_back_to_retry_until_it_succeeds() {
        // Arrange
        var doFail = new AtomicBoolean(true);
        TopicConsumer<String, String> failOnMessage = consumerRecord -> {
            if (consumerRecord.value().equals("value3") && doFail.get()) {
                return ConsumeStatus.FAILED;
            }
            return ConsumeStatus.OK;
        };
        Properties properties = kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId);
        properties.setProperty(MAX_POLL_RECORDS_CONFIG, "4");
        var config = new KafkaConsumerClientConfig<>(properties, Map.of(testTopicA, failOnMessage));

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();

        for (int i = 1; i <= 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }
        producer.flush();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 2, 0));

        // Act
        doFail.set(false);

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));

        assertEquals(5, getCommittedOffsets(testTopicA, 0).offset());

        consumerClient.stop();
    }

    @Test
    public void should_commit_on_both_clients() {
        // Arrange
        var counter1 = new AtomicInteger();
        var counter2 = new AtomicInteger();

        long pollDurationMillis = 200L;

        var config1 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter1, 10)),
                pollDurationMillis);

        var config2 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter2, 0)),
                pollDurationMillis);

        consumerClient = new KafkaConsumerClientImpl<>(config1);
        consumerClient2 = new KafkaConsumerClientImpl<>(config2);

        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }
        producer.flush();
        // Act
        consumerClient.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counter1.get() >= 5);
        consumerClient.stop();
        consumerClient2.start();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 100, 0));

        // Act
        consumerClient2.stop();

        // Assert
        assertTrue(counter2.get() > 0);
        assertEquals(100, counter1.get() + counter2.get());
    }

    @Test
    public void should_consume_on_1_thread_pr_partition() {
        // Arrange
        var counter1 = new AtomicInteger();
        var counter2 = new AtomicInteger();

        var config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(
                        testTopicA, consumerWithCounterAndSleep(counter1, 0),
                        testTopicB, consumerWithCounterAndSleep(counter2, 0)),
                10);

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
            producer.send(new ProducerRecord<>(testTopicB, "key1", "value" + i));
        }

        producer.flush();

        consumerClient = new KafkaConsumerClientImpl<>(config);
        // Act
        consumerClient.start();

        // Assert
        await().atMost(Duration.ofSeconds(5)).until(() -> counter1.get() + counter2.get() == 10);
        consumerClient.stop();
    }

    private OffsetAndMetadata getCommittedOffsets(String topic, int partition) {
        var topicPartition = new TopicPartition(topic, partition);
        Map<TopicPartition, OffsetAndMetadata> committed =
                commitChecker.committed(Set.of(topicPartition), Duration.ofSeconds(1));
        return committed.get(topicPartition);
    }

    private boolean assertCommittedOffsetEquals(String topic, long offset, int partition) {
        var commitedOffset = getCommittedOffsets(topic, partition);
        if (commitedOffset == null) {
            return false;
        } else {
            return commitedOffset.offset() == offset;
        }
    }

    private TopicConsumer<String, String> consumerWithCounterAndSleep(AtomicInteger counter, long sleep) {
        return record -> {
            try {
                Thread.sleep(sleep);
                counter.incrementAndGet();
            } catch (Exception ignored) {
            }
            return ConsumeStatus.OK;
        };
    }
}
