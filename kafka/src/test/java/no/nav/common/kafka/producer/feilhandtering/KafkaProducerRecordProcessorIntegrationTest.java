package no.nav.common.kafka.producer.feilhandtering;

import java.util.*;
import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig;
import no.nav.common.kafka.consumer.KafkaConsumerClientImpl;
import no.nav.common.kafka.producer.feilhandtering.publisher.BatchedKafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.publisher.QueuedKafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.util.KafkaProducerRecordProcessorBuilder;
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
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static no.nav.common.kafka.utils.TestUtils.*;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class KafkaProducerRecordProcessorIntegrationTest {
    private static final long POLL_TIMEOUT = 100;
    private static final long WAIT_TIMEOUT = 500;

    private String testTopicA;
    private String testTopicB;

    public static KafkaContainer kafka;
    private static String brokerUrl;
    private static AdminClient admin;

    private static DataSource dataSource;
    private static KafkaProducerRepository producerRepository;

    private List<String> sentOnTopicA;
    private List<String> sentOnTopicB;

    private KafkaConsumerClient consumerClient;

    private TestKafkaProducerClient producerClient;

    @SneakyThrows
    @BeforeClass
    public static void setup() {
        kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        kafka.waitingFor(new HostPortWaitStrategy());
        kafka.start();

        brokerUrl = kafka.getBootstrapServers();

        dataSource = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(dataSource, "kafka-producer-record-oracle.sql");
        producerRepository = new OracleJdbcTemplateProducerRepository(new JdbcTemplate(dataSource));

        admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
    }

    @AfterClass
    public static void tearDown() {
        admin.close();
        kafka.stop();
    }

    @Before
    public void beforeEach() {
        producerClient = new TestKafkaProducerClient(kafkaTestByteProducerProperties(kafka.getBootstrapServers()));
        testTopicA = UUID.randomUUID().toString();
        testTopicB = UUID.randomUUID().toString();
        admin.createTopics(List.of(
                new NewTopic(testTopicA, 1, (short) 1),
                new NewTopic(testTopicB, 1, (short) 1)
        ));

        sentOnTopicA = new ArrayList<>();
        sentOnTopicB = new ArrayList<>();

        KafkaConsumerClientConfig<String, String> config = new KafkaConsumerClientConfig<>(
                kafkaTestConsumerProperties(kafka.getBootstrapServers()),
                Map.of(
                        testTopicA, r -> {
                            sentOnTopicA.add(r.value());
                            return ConsumeStatus.OK;
                        },
                        testTopicB, r -> {
                            sentOnTopicB.add(r.value());
                            return ConsumeStatus.OK;
                        }
                )
        );
        consumerClient = new KafkaConsumerClientImpl<>(config);
    }

    @After
    public void afterEach() {
        DbUtils.cleanupProducer(dataSource);
        producerClient.close();
    }

    @Test
    public void should_send_stored_records_to_kafka() {
        producerRepository.storeRecord(storedRecord(testTopicA, "key1", "value1"));
        producerRepository.storeRecord(storedRecord(testTopicA, "key2", "value2"));
        producerRepository.storeRecord(storedRecord(testTopicA, "key1", "value3"));

        producerRepository.storeRecord(storedRecord(testTopicB, "key1", "value1"));
        producerRepository.storeRecord(storedRecord(testTopicB, "key2", "value2"));

        var recordProcessor = KafkaProducerRecordProcessorBuilder.builder()
                .withProducerRepository(producerRepository)
                .withLeaderElectionClient(() -> true)
                .withRecordPublisher(new BatchedKafkaProducerRecordPublisher(producerClient))
                .withPollTimeoutMs(POLL_TIMEOUT)
                .build();

        recordProcessor.start();
        consumerClient.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> sentOnTopicA.size() == 3 && sentOnTopicB.size() == 2);
        recordProcessor.close();
        consumerClient.stop();

        assertTrue(producerRepository.getRecords(10).isEmpty());
    }

    @Test
    public void should_send_batch_of_records_with_best_effort_of_delivery() {
        producerRepository.storeRecord(storedRecord(testTopicA, "k-1", "a-1"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-2", "a-2"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-3", "a-3"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-1", "b-1"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-2", "b-2"));

        // Simulate a producer that fails on the second message to TOPIC_A
        producerClient.setOnSend(producerRecord -> {
            String value = new String(producerRecord.value(), StandardCharsets.UTF_8);
            if (value.equals("a-2")) {
                throw new RuntimeException("Simulated error");
            }
        });

        var recordProcessor = KafkaProducerRecordProcessorBuilder.builder()
                .withProducerRepository(producerRepository)
                .withLeaderElectionClient(() -> true)
                .withRecordPublisher(new BatchedKafkaProducerRecordPublisher(producerClient))
                .withPollTimeoutMs(POLL_TIMEOUT)
                .build();

        recordProcessor.start();
        consumerClient.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> sentOnTopicB.size() == 2);
        recordProcessor.close();
        consumerClient.stop();

        // We expect most of the messages to have been sent
        assertEquals(Set.of("a-1", "a-3"), Set.copyOf(sentOnTopicA));
        assertEquals(Set.of("b-1", "b-2"), Set.copyOf(sentOnTopicB));

        // But the batch of messages is still available in the repository since of the messages failed
        List<StoredProducerRecord> records = producerRepository.getRecords(10);
        assertEquals(5, records.size());
    }

    @Test
    public void legacy_constructor_uses_batch_processor() {
        producerRepository.storeRecord(storedRecord(testTopicA, "k-1", "a-1"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-2", "a-2"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-3", "a-3"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-1", "b-1"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-2", "b-2"));

        // Simulate a producer that fails on the second message to TOPIC_A
        producerClient.setOnSend(producerRecord -> {
            String value = new String(producerRecord.value(), StandardCharsets.UTF_8);
            if (value.equals("a-2")) {
                throw new RuntimeException("Simulated error");
            }
        });

        var recordProcessor = new KafkaProducerRecordProcessor(
                producerRepository,
                producerClient,
                () -> true
        );

        recordProcessor.start();
        consumerClient.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> sentOnTopicB.size() == 2);
        recordProcessor.close();
        consumerClient.stop();

        // We expect most of the messages to have been sent
        assertEquals(Set.of("a-1", "a-3"), Set.copyOf(sentOnTopicA));
        assertEquals(Set.of("b-1", "b-2"), Set.copyOf(sentOnTopicB));

        // But the batch of messages is still available in the repository since of the messages failed
        List<StoredProducerRecord> records = producerRepository.getRecords(10);
        assertEquals(5, records.size());
    }


    @Test
    public void should_send_records_in_the_stored_record_order_from_the_producer_record_repository() throws InterruptedException {
        producerRepository.storeRecord(storedRecord(testTopicA, "k-1", "a-1"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-1", "b-1"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-2", "a-2"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-2", "b-2"));
        producerRepository.storeRecord(storedRecord(testTopicA, "k-3", "a-3"));
        producerRepository.storeRecord(storedRecord(testTopicB, "k-3", "b-3"));

        // Simulate a producer that fails on the second message to TOPIC_B
        producerClient.setOnSend(producerRecord -> {
            String value = new String(producerRecord.value(), StandardCharsets.UTF_8);
            if (value.equals("b-2")) {
                throw new RuntimeException("Simulated error");
            }
        });

        var recordProcessor = KafkaProducerRecordProcessorBuilder.builder()
                .withProducerRepository(producerRepository)
                .withLeaderElectionClient(() -> true)
                .withRecordPublisher(new QueuedKafkaProducerRecordPublisher(producerClient))
                .withPollTimeoutMs(POLL_TIMEOUT)
                .build();

        recordProcessor.start();
        consumerClient.start();

        // Wait for all messages to be consumed
        Thread.sleep(WAIT_TIMEOUT);

        recordProcessor.close();
        consumerClient.stop();

        // We expect all messages before the failed record to have been sent and consumed
        assertEquals(List.of("a-1", "a-2"), sentOnTopicA);
        assertEquals(List.of("b-1"), sentOnTopicB);

        // And the failed record and the following records to still be in the repository
        List<StoredProducerRecord> records = producerRepository.getRecords(10);
        assertEquals(3, records.size());
        assertEquals("b-2", new String(records.get(0).getValue(), StandardCharsets.UTF_8));
        assertEquals("a-3", new String(records.get(1).getValue(), StandardCharsets.UTF_8));
        assertEquals("b-3", new String(records.get(2).getValue(), StandardCharsets.UTF_8));
    }


    @Test
    public void should_not_send_records_to_kafka_stored_in_a_transaction_that_gets_rolled_back() throws InterruptedException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        var recordProcessor = KafkaProducerRecordProcessorBuilder.builder()
                .withProducerRepository(producerRepository)
                .withLeaderElectionClient(() -> true)
                .withRecordPublisher(new BatchedKafkaProducerRecordPublisher(producerClient))
                .withPollTimeoutMs(POLL_TIMEOUT)
                .build();

        consumerClient.start();
        recordProcessor.start();

        transactionTemplate.execute(status -> {
            producerRepository.storeRecord(storedRecord(testTopicA, "value1", "key1"));
            try {
                Thread.sleep(WAIT_TIMEOUT);
            } catch (InterruptedException ignored) {
            }
            status.setRollbackOnly();
            return null;
        });
        Thread.sleep(WAIT_TIMEOUT);

        recordProcessor.close();
        consumerClient.stop();
        assertEquals(0, sentOnTopicA.size());
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
