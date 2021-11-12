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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerClientTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> commitChecker;

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @Before
    public void setup() {
        String brokerUrl = kafka.getBootstrapServers();
        producer = new KafkaProducer<>(kafkaTestProducerProperties(brokerUrl));
        commitChecker = new KafkaConsumer<>(kafkaTestConsumerProperties(brokerUrl));

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));

        admin.deleteTopics(List.of(TEST_TOPIC_A, TEST_TOPIC_B));

        admin.createTopics(List.of(
                new NewTopic(TEST_TOPIC_A, 1, (short) 1),
                new NewTopic(TEST_TOPIC_B, 1, (short) 1)
        ));

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
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_A, consumerWithCounter(counter, 0))
        );

        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value1"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value2"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key2", "value3"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key2", null));
        producer.send(new ProducerRecord<>(TEST_TOPIC_A, null, "value5"));

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_A, 0);

        assertEquals(5, committedOffsets.offset());
        assertEquals(5, counter.get());

        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value4"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key2", "value5"));

        producer.flush();

        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets2 = getCommittedOffsets(TEST_TOPIC_A, 0);

        assertEquals(7, committedOffsets2.offset());
        assertEquals(7, counter.get());
    }

    @Test
    public void should_commit_consumed_tasks_when_closed_gracefully() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 15; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value" + i));
        }

        producer.flush();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_A, consumerWithCounter(counter, 100)),
                10
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_A, 0);

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
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(multiPartitionTopic, consumerWithCounter(counter, 0))
        );

        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 0, "key1", "value3"));

        producer.send(new ProducerRecord<>(multiPartitionTopic, 1,"key2", "value1"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value2"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value3"));
        producer.send(new ProducerRecord<>(multiPartitionTopic, 1, "key2", "value4"));

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets0 = getCommittedOffsets(multiPartitionTopic, 0);

        OffsetAndMetadata committedOffsets1 = getCommittedOffsets(multiPartitionTopic, 1);

        assertEquals(7, counter.get());
        assertEquals(3, committedOffsets0.offset());
        assertEquals(4, committedOffsets1.offset());
    }

    @Test
    public void should_stop_consumption_of_topic_on_failure() throws InterruptedException {
        AtomicInteger messagesReadCounterA = new AtomicInteger();
        AtomicInteger messagesReadCounterB = new AtomicInteger();
        AtomicLong maxRecordOffsetB = new AtomicLong(-1);

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(
                        TEST_TOPIC_A, consumerWithCounter(messagesReadCounterA, 0),
                        TEST_TOPIC_B, failOnOffsetConsumer(maxRecordOffsetB, messagesReadCounterB, 2)
                )
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
        consumerClient.start();

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value" + i));
            producer.send(new ProducerRecord<>(TEST_TOPIC_B, "key1", "value" + i));
        }

        producer.flush();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets1 = getCommittedOffsets(TEST_TOPIC_A, 0);

        OffsetAndMetadata committedOffsets2 = getCommittedOffsets(TEST_TOPIC_B, 0);

        assertEquals(5, messagesReadCounterA.get());
        assertEquals(5, committedOffsets1.offset());

        assertEquals(2, maxRecordOffsetB.get());
        assertTrue("Should have been reading messages", messagesReadCounterB.get() >= 3);
        assertEquals(2, committedOffsets2.offset());
    }

    @Test
    public void should_stop_consumption_of_topic_on_and_seek_back_to_retry_until_it_succeeds() throws InterruptedException {
        AtomicBoolean doFail = new AtomicBoolean(true);
        Properties properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());
        properties.setProperty(MAX_POLL_RECORDS_CONFIG, "4");
        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                properties,
                Map.of(
                        TEST_TOPIC_A, failOnMessage("value3", doFail)
                )
        );

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
        consumerClient.start();

        for (int i = 1; i <= 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value" + i));
        }
        producer.flush();

        Thread.sleep(1000);
        assertEquals(2, getCommittedOffsets(TEST_TOPIC_A, 0).offset());

        doFail.set(false);

        Thread.sleep(1000);
        assertEquals(5, getCommittedOffsets(TEST_TOPIC_A, 0).offset());

        consumerClient.stop();


    }

    @Test
    public void should_commit_on_both_clients() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config1 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_A, consumerWithCounter(counter1, 100))
        );

        KafkaConsumerClientConfig<String, String> config2 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_A, consumerWithCounter(counter2, 100))
        );

        KafkaConsumerClientImpl<String, String> consumerClient1 = new KafkaConsumerClientImpl<>(config1, null);
        KafkaConsumerClientImpl<String, String> consumerClient2 = new KafkaConsumerClientImpl<>(config2, null);

        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value" + i));
        }

        producer.flush();

        consumerClient1.start();

        Thread.sleep(1000);

        consumerClient2.start();

        Thread.sleep(1000);

        consumerClient1.stop();

        Thread.sleep(1000);

        consumerClient2.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_A, 0);

        assertTrue(committedOffsets.offset() > 10);
        assertTrue(counter1.get() > 5);
        assertTrue(counter2.get() > 5);
    }

    @Test
    public void should_consume_on_1_thread_pr_partition() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(
                        TEST_TOPIC_A, consumerWithCounter(counter1, 100),
                        TEST_TOPIC_B, consumerWithCounter(counter2, 100)
                ),
                10
        );

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_A, "key1", "value" + i));
            producer.send(new ProducerRecord<>(TEST_TOPIC_B, "key1", "value" + i));
        }

        producer.flush();

        KafkaConsumerClientImpl<String, String> consumerClient = new KafkaConsumerClientImpl<>(config, null);
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

    private TopicConsumer<String, String> consumerWithCounter(AtomicInteger counter, long sleep) {
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
