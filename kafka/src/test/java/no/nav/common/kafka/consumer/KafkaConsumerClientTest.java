package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerClientTest {

    private String testTopicA;
    private String testTopicB;

    private String consumerGroupId;

    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> commitChecker;



    private static KafkaContainer kafka;

    @BeforeClass
    public static void beforeClass() {
        kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafka.waitingFor(new HostPortWaitStrategy());
        kafka.start();
    }

    @AfterClass
    public static void afterClass() {
        kafka.stop();
    }

    @Before
    public void setup() {
        consumerGroupId = UUID.randomUUID().toString();
        String brokerUrl = kafka.getBootstrapServers();
        producer = new KafkaProducer<>(kafkaTestProducerProperties(brokerUrl));
        commitChecker = new KafkaConsumer<>(kafkaTestConsumerProperties(brokerUrl, consumerGroupId));

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        testTopicA = UUID.randomUUID().toString();
        testTopicB = UUID.randomUUID().toString();

        admin.close(); // Apply changes
    }

    @After
    public void cleanup() {
        producer.close();
        commitChecker.close();
    }

    @Test
    public void should_consume_and_commit_offsets_and_start_consuming_again_on_excepted_offset() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter, 0))
        );

        producer.send(new ProducerRecord<>(testTopicA, "key1", "value1"));
        producer.send(new ProducerRecord<>(testTopicA, "key1", "value2"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", "value3"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", null));
        producer.send(new ProducerRecord<>(testTopicA, null, "value5"));

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));

        consumerClient.stop();

        assertEquals(5, counter.get());

        producer.send(new ProducerRecord<>(testTopicA, "key1", "value4"));
        producer.send(new ProducerRecord<>(testTopicA, "key2", "value5"));

        producer.flush();

        consumerClient.start();

        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 7, 0));

        consumerClient.stop();

        assertEquals(7, counter.get());
    }



    @Test
    public void should_commit_consumed_tasks_when_closed_gracefully() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 15; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }

        producer.flush();

        // Slow consumer (100ms wait)
        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter, 100)),
                10
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        Thread.sleep(1000);

        // Stop client after approx 10 messages
        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(testTopicA, 0);

        assertTrue(committedOffsets.offset() > 4);
        assertTrue(committedOffsets.offset() < 12);
        assertEquals(counter.get(), committedOffsets.offset());
    }

    @Test
    public void should_consume_from_several_partitions() throws InterruptedException {
        String multiPartitionTopic = "multi-partition-topic";

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
        admin.createTopics(List.of(new NewTopic(multiPartitionTopic, 2, (short) 1)));
        admin.close(); // Apply changes

        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(multiPartitionTopic, consumerWithCounterAndSleep(counter, 0))
        );

        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value3"));

        producer.send(new ProducerRecord<>(multiPartitionTopic, 1,"key2", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value3"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value4"));

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        assertCommittedOffsetEquals(multiPartitionTopic, 3, 0);
        assertCommittedOffsetEquals(multiPartitionTopic, 4, 1);

        await().atMost(Duration.ofSeconds(5)).until(() -> counter.get() == 7);
        consumerClient.stop();
    }

    @Test
    public void should_stop_consumption_of_topic_on_failure() throws InterruptedException {
        AtomicInteger messagesReadCounterA = new AtomicInteger();
        AtomicInteger messagesReadCounterB = new AtomicInteger();
        AtomicLong maxRecordOffsetB = new AtomicLong(-1);

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(
                        testTopicA, consumerWithCounterAndSleep(messagesReadCounterA, 0),
                        testTopicB, failOnOffsetConsumer(maxRecordOffsetB, messagesReadCounterB, 2)
                )
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
            producer.send(new ProducerRecord<>(testTopicB, "key1", "value" + i));
        }

        producer.flush();

        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));
        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicB, 2, 0));

        await().atMost(Duration.ofSeconds(5)).until(() -> messagesReadCounterA.get() == 5);
        await().atMost(Duration.ofSeconds(5)).until(() -> maxRecordOffsetB.get() == 2);
        assertTrue("Should have been reading messages", messagesReadCounterB.get() >= 3);

        consumerClient.stop();
    }

    @Test
    public void should_stop_consumption_of_topic_on_and_seek_back_to_retry_until_it_succeeds() throws InterruptedException {
        AtomicBoolean doFail = new AtomicBoolean(true);
        Properties properties = kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId);
        properties.setProperty(MAX_POLL_RECORDS_CONFIG, "4");
        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                properties,
                Map.of(
                        testTopicA, failOnMessage("value3", doFail)
                )
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        for (int i = 1; i <= 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }
        producer.flush();

        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 2, 0));
    ;
        doFail.set(false);

        await().atMost(Duration.ofSeconds(5)).until(() -> assertCommittedOffsetEquals(testTopicA, 5, 0));

        assertEquals(5, getCommittedOffsets(testTopicA, 0).offset());

        consumerClient.stop();


    }

    @Test
    public void should_commit_on_both_clients() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        long pollDurationMillis = 200L;

        KafkaConsumerClientConfig<String, String> config1 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter1, 10)),
                pollDurationMillis
        );

        KafkaConsumerClientConfig<String, String> config2 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(testTopicA, consumerWithCounterAndSleep(counter2, 0)),
                pollDurationMillis
        );

        KafkaConsumerClientImpl<String, String> consumerClient1 = new KafkaConsumerClientImpl<>(config1);
        KafkaConsumerClientImpl<String, String> consumerClient2 = new KafkaConsumerClientImpl<>(config2);




        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
        }
        producer.flush();
        consumerClient1.start();
        Thread.sleep(1000);
        consumerClient2.start();

        consumerClient1.stop();


        await().atMost(Duration.ofSeconds(5)).until( () -> assertCommittedOffsetEquals(testTopicA, 100, 0));


        consumerClient2.stop();

        assertTrue(counter1.get() > 5);
        assertTrue(counter2.get() > 5);
        assertTrue(counter1.get() + counter2.get() == 100);
    }

    @Test
    public void should_consume_on_1_thread_pr_partition() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers(), consumerGroupId),
                Map.of(
                        testTopicA, consumerWithCounterAndSleep(counter1, 100),
                        testTopicB, consumerWithCounterAndSleep(counter2, 100)
                ),
                10
        );

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(testTopicA, "key1", "value" + i));
            producer.send(new ProducerRecord<>(testTopicB, "key1", "value" + i));
        }

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config);
        consumerClient.start();

        Thread.sleep(750);

        assertTrue(8 < counter1.get() + counter2.get());

        consumerClient.stop();
    }

    private OffsetAndMetadata getCommittedOffsets(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        Map<TopicPartition, OffsetAndMetadata> committed = commitChecker.committed(Set.of(topicPartition), Duration.ofSeconds(1));
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

    private boolean assertCommittedOffsetMinimumValue(String topic, long offset, int partition) {
        var commitedOffset = getCommittedOffsets(topic, partition);
        if (commitedOffset == null) {
            return false;
        } else {
            return commitedOffset.offset() >= offset;
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


    private TopicConsumer<String, String> failOnOffsetConsumer(AtomicLong maxRecord, AtomicInteger counter, int offset) {
        return record -> {
            counter.incrementAndGet();
            maxRecord.set(Math.max(maxRecord.get(), record.offset()));

            return record.offset() == offset
                    ? ConsumeStatus.FAILED
                    : ConsumeStatus.OK;
        };
    }

    private TopicConsumer<String, String> failOnMessage(String message, AtomicBoolean doFail) {
        return record -> {
            if (record.value().equals(message) && doFail.get()) {
                return ConsumeStatus.FAILED;
            }
            return ConsumeStatus.OK;
        };
    }
}
