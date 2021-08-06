package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.KafkaConsumerClientImpl;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import no.nav.common.kafka.spring.OracleJdbcTemplateProducerRepository;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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

    private KafkaProducerRecordProcessor recordProcessor;

    private KafkaConsumerClientImpl<String, String> consumerClient;

    private AtomicInteger counterTopicA = new AtomicInteger();

    private AtomicInteger counterTopicB = new AtomicInteger();

    @Before
    public void setup() {
        counterTopicA.set(0);
        counterTopicB.set(0);

        String brokerUrl = kafka.getBootstrapServers();

        dataSource = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(dataSource, "kafka-producer-record-oracle.sql");
        producerRepository = new OracleJdbcTemplateProducerRepository(new JdbcTemplate(dataSource));

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        admin.deleteTopics(List.of(TEST_TOPIC_A, TEST_TOPIC_B));
        admin.createTopics(List.of(
                new NewTopic(TEST_TOPIC_A, 1, (short) 1),
                new NewTopic(TEST_TOPIC_B, 1, (short) 1)
        ));
        admin.close(); // Apply changes

        KafkaProducerClient<byte[], byte[]> producer = new KafkaProducerClientImpl<>(kafkaTestByteProducerProperties(kafka.getBootstrapServers()));
        LeaderElectionClient leaderElectionClient = () -> true;

        recordProcessor = new KafkaProducerRecordProcessor(
                producerRepository,
                producer,
                leaderElectionClient
        );

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

        consumerClient = new KafkaConsumerClientImpl<>(config);
    }

    @After
    public void cleanup() {
        DbUtils.cleanupProducer(dataSource);
    }

    @Test
    public void should_send_stored_records_to_kafka() throws InterruptedException {
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value1", "key1"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value2", "key2"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value3", "key1"));

        producerRepository.storeRecord(storedRecord(TEST_TOPIC_B, "value1", "key1"));
        producerRepository.storeRecord(storedRecord(TEST_TOPIC_B, "value2", "key2"));

        recordProcessor.start();
        Thread.sleep(1000);
        recordProcessor.close();

        consumerClient.start();
        Thread.sleep(1000);
        consumerClient.stop();

        assertEquals(3, counterTopicA.get());
        assertEquals(2, counterTopicB.get());
        assertTrue(producerRepository.getRecords(10).isEmpty());
    }

    @Test
    public void should_not_send_records_to_kafka_stored_in_a_transaction_that_gets_rolled_back() throws InterruptedException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        consumerClient.start();
        recordProcessor.start();

        transactionTemplate.execute(status -> {
            producerRepository.storeRecord(storedRecord(TEST_TOPIC_A, "value1", "key1"));
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {
            }
            status.setRollbackOnly();
            return null;
        });
        Thread.sleep(4000);

        recordProcessor.close();
        consumerClient.stop();
        assertEquals(0, counterTopicA.get());
    }


    private StoredProducerRecord storedRecord(String topic, String key, String value) {
        return new StoredProducerRecord(
                topic,
                key.getBytes(),
                value.getBytes(),
                "[]"
        );
    }

}
