package no.nav.common.kafka.consumer.feilhandtering;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository;
import no.nav.common.kafka.utils.DbUtils;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresDataSource;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerRecordProcessorIntegrationTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    private static DataSource dataSource;

    private KafkaConsumerRepository consumerRepository;

    private static Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        return Optional.of(() -> {
        });
    }

    @ClassRule
    public static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:12-alpine")
                    .withInitScript("kafka-consumer-record-postgres.sql")
                    .waitingFor(new HostPortWaitStrategy());

    @Before
    public void setup() {
        dataSource = createPostgresDataSource(postgreSQLContainer);
        consumerRepository = new PostgresJdbcTemplateConsumerRepository(new JdbcTemplate(dataSource));
    }

    @After
    public void after() {
        DbUtils.cleanupConsumer(dataSource);
    }

    @Test
    public void should_consume_stored_records() {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                ),
                new TopicConsumerConfig<>(
                        TEST_TOPIC_B,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicB.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        consumerRecordProcessor.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counterTopicA.get() == 3 && counterTopicB.get() == 2);
        consumerRecordProcessor.stop();

        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).isEmpty());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
    }

    @Test
    public void should_support_multiple_consumers_for_the_same_topic() {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});

        AtomicInteger counterConsumer1 = new AtomicInteger();
        AtomicInteger counterConsumer2 = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterConsumer1.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                ),
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterConsumer2.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value1"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value2"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value3"));

        consumerRecordProcessor.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counterConsumer1.get() == 3 && counterConsumer2.get() == 3);
        consumerRecordProcessor.stop();

        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).isEmpty());
    }

    @Test
    public void should_only_consume_from_topics_when_lock_is_acquired() {
        LockProvider lockProvider = lockConfiguration -> {
            if (lockConfiguration.getName().contains(TEST_TOPIC_B)) {
                return Optional.of((SimpleLock) () -> {
                });
            }

            return Optional.empty();
        };

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                ),
                new TopicConsumerConfig<>(
                        TEST_TOPIC_B,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicB.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        consumerRecordProcessor.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counterTopicB.get() == 2);
        consumerRecordProcessor.stop();

        assertEquals(0, counterTopicA.get());

        assertEquals(2, consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).size());
        assertEquals(1, consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).size());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
    }

    @Test
    public void should_not_block_processing_when_null_keys() {
        LockProvider lockProvider = KafkaConsumerRecordProcessorIntegrationTest::lock;

        AtomicInteger counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, null, "value1"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, null, "value2"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 3, null, "value3"));

        consumerRecordProcessor.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counterTopicA.get() == 3);
        consumerRecordProcessor.stop();
        assertEquals(3, counterTopicA.get());
    }

    @Test
    public void should_consume_records_if_backoff_expired() {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});
        AtomicInteger counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .withBackoffStrategy((r) -> Duration.ofSeconds(-1))
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        List<StoredConsumerRecord> records = consumerRepository.getRecords(TEST_TOPIC_A, 1, 5);
        records.forEach(record -> consumerRepository.incrementRetries(record.getId()));

        consumerRecordProcessor.start();
        await().atMost(Duration.ofSeconds(5)).until(() -> counterTopicA.get() == 2);
        consumerRecordProcessor.stop();

        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
    }

    @Test
    public void should_not_consume_records_if_backoff_not_expired() throws InterruptedException {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});
        AtomicInteger counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .withBackoffStrategy((r) -> Duration.ofHours(1))
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        List<StoredConsumerRecord> records = consumerRepository.getRecords(TEST_TOPIC_A, 1, 5);
        records.forEach(record -> consumerRepository.incrementRetries(record.getId()));

        consumerRecordProcessor.start();
        Thread.sleep(1000);
        consumerRecordProcessor.stop();

        assertEquals(0, counterTopicA.get());
        assertEquals(2, consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).size());
    }

    private StoredConsumerRecord storedRecord(String topic, int partition, long offset, String key, String value) {
        return new StoredConsumerRecord(
                topic,
                partition,
                offset,
                key != null ? key.getBytes() : null,
                value.getBytes(),
                "[]",
                System.currentTimeMillis()
        );
    }

}
