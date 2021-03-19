package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.producer.GracefulKafkaProducer;
import no.nav.common.kafka.utils.LocalH2Database;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaProducerRecordProcessorIntegrationTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    private DataSource dataSource;

    private KafkaProducerRepository producerRepository;

    @Before
    public void setup() {
        String brokerUrl = kafka.getBootstrapServers();

        dataSource = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.init(dataSource, "kafka-producer-record-postgres.sql");
        producerRepository = new PostgresProducerRepository(dataSource);

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
        LocalH2Database.cleanupProducer(dataSource);
    }

    @Test
    public void should_send_stored_records_to_kafka() throws InterruptedException {
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value1", "key1"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value2", "key2"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value3", "key1"));

        producerRepository.storeRecord(storedRecord(TEST_TOPIC_B, "value1", "key1"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_B, "value2", "key2"));

        KafkaProducer<byte[], byte[]> producer = new GracefulKafkaProducer<>(kafkaTestByteProducerProperties(kafka.getBootstrapServers()));
        LeaderElectionClient leaderElectionClient = () -> true;

        KafkaProducerRecordProcessor recordProcessor = new KafkaProducerRecordProcessor(
                producerRepository,
                producer,
                leaderElectionClient
        );

        recordProcessor.start();
        Thread.sleep(1000);
        recordProcessor.close();

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(
                        TEST_TOPIC_A, (r) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        },
                        TEST_TOPIC_B, (r) -> {
                            counterTopicB.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerClient<String, String> consumerClient = new KafkaConsumerClient<>(config);

        consumerClient.start();
        Thread.sleep(1000);
        consumerClient.stop();

        assertEquals(3, counterTopicA.get());
        assertEquals(2, counterTopicB.get());
        assertTrue(producerRepository.getRecords(10).isEmpty());
    }

    private StoredProducerRecord storedRecord(String topic, String value, String key) {
        return new StoredProducerRecord(
                topic,
                key.getBytes(),
                value.getBytes(),
                "[]"
        );
    }

}
