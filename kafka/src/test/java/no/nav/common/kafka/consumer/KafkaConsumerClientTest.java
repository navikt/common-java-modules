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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerClientTest {

    private final static String TEST_TOPIC_1 = "test-topic-a";

    private final static String TEST_TOPIC_2 = "test-topic-b";

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

        admin.deleteTopics(List.of(TEST_TOPIC_1, TEST_TOPIC_2));

        admin.createTopics(List.of(
                new NewTopic(TEST_TOPIC_1, 1, (short) 1),
                new NewTopic(TEST_TOPIC_2, 1, (short) 1)
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
                Map.of(TEST_TOPIC_1, consumerWithCounter(counter, 0))
        );

        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value1"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value2"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key2", "value3"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key2", null));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, null, "value5"));

        producer.flush();

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_1, 0);

        assertEquals(5, committedOffsets.offset());
        assertEquals(5, counter.get());

        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value4"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key2", "value5"));

        producer.flush();

        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets2 = getCommittedOffsets(TEST_TOPIC_1, 0);

        assertEquals(7, committedOffsets2.offset());
        assertEquals(7, counter.get());
    }

    @Test
    public void should_commit_consumed_tasks_when_closed_gracefully() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 15; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
        }

        producer.flush();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_1, consumerWithCounter(counter, 100)),
                10
        );

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_1, 0);

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

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
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
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(
                        TEST_TOPIC_1, consumerWithCounter(counter1, 0),
                        TEST_TOPIC_2, failOnCountConsumer(counter2, 3)
                )
        );

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
            producer.send(new ProducerRecord<>(TEST_TOPIC_2, "key1", "value" + i));
        }

        producer.flush();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets1 = getCommittedOffsets(TEST_TOPIC_1, 0);

        OffsetAndMetadata committedOffsets2 = getCommittedOffsets(TEST_TOPIC_2, 0);

        assertEquals(5, counter1.get());
        assertEquals(5, committedOffsets1.offset());

        assertEquals(3, counter2.get());
        assertEquals(2, committedOffsets2.offset());
    }

    @Test
    public void should_commit_on_both_clients() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config1 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_1, consumerWithCounter(counter1, 100))
        );

        KafkaConsumerClientConfig<String, String> config2 = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(TEST_TOPIC_1, consumerWithCounter(counter2, 100))
        );

        KafkaConsumerClient<String, String> consumerClient1 = new KafkaConsumerClient<>(config1);
        KafkaConsumerClient<String, String> consumerClient2 = new KafkaConsumerClient<>(config2);

        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
        }

        producer.flush();

        consumerClient1.start();

        Thread.sleep(1000);

        consumerClient2.start();

        Thread.sleep(1000);

        consumerClient1.stop();

        Thread.sleep(1000);

        consumerClient2.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_1, 0);

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
                        TEST_TOPIC_1, consumerWithCounter(counter1, 100),
                        TEST_TOPIC_2, consumerWithCounter(counter2, 100)
                ),
                10
        );

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
            producer.send(new ProducerRecord<>(TEST_TOPIC_2, "key1", "value" + i));
        }

        producer.flush();

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
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

    private TopicConsumer<String, String> failOnCountConsumer(AtomicInteger counter, int failOnCount) {
        return record -> {
            int count = counter.incrementAndGet();

            return count == failOnCount
                    ? ConsumeStatus.FAILED
                    : ConsumeStatus.OK;
        };
    }

}
