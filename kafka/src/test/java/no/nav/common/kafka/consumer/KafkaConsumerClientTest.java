package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerClientTest {

    private final static String TEST_TOPIC_1 = "test-topic-a";

    private final static String TEST_TOPIC_2 = "test-topic-b";

    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> commitChecker;

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

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
    public void should_consume_and_commit_offsets() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>();
        config.topics = Map.of(TEST_TOPIC_1, consumerWithCounter(counter, 0));
        config.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value1"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value2"));
        producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key2", "value3"));

        producer.flush();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_1, 0);

        assertEquals(2, committedOffsets.offset());
        assertEquals(3, counter.get());
    }

    @Test
    public void should_commit_consumed_tasks_when_closed_gracefully() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>();
        config.topics = Map.of(TEST_TOPIC_1, consumerWithCounter(counter, 100));
        config.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        for (int i = 0; i < 15; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
        }

        producer.flush();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets = getCommittedOffsets(TEST_TOPIC_1, 0);

        assertEquals(9, committedOffsets.offset()); // This might be a bit brittle when running on CI/CD
        assertEquals(counter.get(), committedOffsets.offset() + 1);
    }

    @Test
    public void should_consume_from_several_partitions() throws InterruptedException {
        String multiPartitionTopic = "multi-partition-topic";

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
        admin.createTopics(List.of(new NewTopic(multiPartitionTopic, 2, (short) 1)));
        admin.close(); // Apply changes

        AtomicInteger counter = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>();
        config.topics = Map.of(multiPartitionTopic, consumerWithCounter(counter, 0));
        config.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

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
        assertEquals(2, committedOffsets0.offset());
        assertEquals(3, committedOffsets1.offset());
    }

    @Test
    public void should_stop_consumption_of_topic_on_failure() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>();
        config.topics = Map.of(
                TEST_TOPIC_1, consumerWithCounter(counter1, 0),
                TEST_TOPIC_2, failOnCountConsumer(counter2, 3)
        );
        config.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);
        consumerClient.start();

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_1, "key1", "value" + i));
        }

        for (int i = 0; i < 5; i++) {
            producer.send(new ProducerRecord<>(TEST_TOPIC_2, "key1", "value" + i));
        }

        producer.flush();

        Thread.sleep(1000);

        consumerClient.stop();

        OffsetAndMetadata committedOffsets1 = getCommittedOffsets(TEST_TOPIC_1, 0);

        OffsetAndMetadata committedOffsets2 = getCommittedOffsets(TEST_TOPIC_2, 0);

        assertEquals(5, counter1.get());
        assertEquals(4, committedOffsets1.offset());

        assertEquals(3, counter2.get());
        assertEquals(1, committedOffsets2.offset());
    }

    @Test
    public void should_commit_on_both_clients() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config1 = new KafkaConsumerClientConfig<>();
        config1.topics = Map.of(TEST_TOPIC_1, consumerWithCounter(counter1, 100));
        config1.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

        KafkaConsumerClientConfig<String, String> config2 = new KafkaConsumerClientConfig<>();
        config2.topics = Map.of(TEST_TOPIC_1, consumerWithCounter(counter2, 100));
        config2.properties = kafkaTestConsumerProperties(kafka.getBootstrapServers());

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

    private Properties kafkaTestProducerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000); // Prøv opptil 3 sekunder på å sende en melding
        return props;
    }

    private Properties kafkaTestConsumerProperties(String brokerUrl) {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5 * 60 * 1000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

}
