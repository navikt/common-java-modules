package no.nav.common.kafka.consumer.feilhandtering;

import static no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresDataSource;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresTestContainer;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.spring.OracleJdbcTemplateConsumerRepository;
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ParameterizedClass(name = "{0}")
@MethodSource("data")
public class KafkaConsumerRecordProcessorIntegrationTest {
    private static final String TEST_TOPIC_A = "test-topic-a";
    private static final String TEST_TOPIC_B = "test-topic-b";

    private final DataSource dataSource;
    private final KafkaConsumerRepository consumerRepository;

    private static Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        return Optional.of(() -> {});
    }

    public static final PostgreSQLContainer postgreSQLContainer = createPostgresTestContainer();
    private static HikariDataSource postgresDataSource;

    public static Collection<Object[]> data() throws Exception {
        postgreSQLContainer.start();
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> postgreSQLContainer.isCreated() && postgreSQLContainer.isRunning());

        postgresDataSource = createPostgresDataSource(postgreSQLContainer);
        DataSource postgres = postgresDataSource;
        DbUtils.runScript(postgres, "kafka-consumer-record-postgres.sql");
        var postgresConsumerRepository = new PostgresJdbcTemplateConsumerRepository(new JdbcTemplate(postgres));

        DataSource oracle = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(oracle, "kafka-consumer-record-oracle.sql");
        DbUtils.runScript(oracle, "oracle-mock.sql");
        var oracleConsumerRepository = new OracleJdbcTemplateConsumerRepository(new JdbcTemplate(oracle));

        return Arrays.asList(
                new Object[] {"POSTGRES", postgres, postgresConsumerRepository},
                new Object[] {"ORACLE", oracle, oracleConsumerRepository});
    }

    // JUnit sender databaseetiketten som et argument, selv om den bare brukes i testnavnet.
    @SuppressWarnings("java:S1172")
    public KafkaConsumerRecordProcessorIntegrationTest(
            @SuppressWarnings("unused") String databaseType,
            DataSource dataSource,
            KafkaConsumerRepository consumerRepository) {
        this.dataSource = dataSource;
        this.consumerRepository = consumerRepository;
    }

    @AfterEach
    public void after() throws Exception {
        DbUtils.cleanupConsumer(dataSource);
    }

    @AfterAll
    public static void stopp_database() {
        try {
            if (postgresDataSource != null) {
                postgresDataSource.close();
            }
        } finally {
            postgreSQLContainer.stop();
        }
    }

    @Test
    public void should_consume_stored_records() {
        // Arrange
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});

        var counterTopicA = new AtomicInteger();
        var counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                }),
                new TopicConsumerConfig<>(TEST_TOPIC_B, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicB.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        // Act
        try {
            consumerRecordProcessor.start();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertEquals(3, counterTopicA.get());
                assertEquals(2, counterTopicB.get());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).isEmpty());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
            });
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    @Test
    public void should_support_multiple_consumers_for_the_same_topic() {
        // Arrange
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});

        var counterConsumer1 = new AtomicInteger();
        var counterConsumer2 = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterConsumer1.incrementAndGet();
                    return ConsumeStatus.OK;
                }),
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterConsumer2.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value1"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value2"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value3"));

        // Act
        try {
            consumerRecordProcessor.start();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertEquals(3, counterConsumer1.get());
                assertEquals(3, counterConsumer2.get());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).isEmpty());
            });
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    @Test
    public void should_only_consume_from_topics_when_lock_is_acquired() {
        // Arrange
        LockProvider lockProvider = lockConfiguration -> {
            if (lockConfiguration.getName().contains(TEST_TOPIC_B)) {
                return Optional.of((SimpleLock) () -> {});
            }

            return Optional.empty();
        };

        var counterTopicA = new AtomicInteger();
        var counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                }),
                new TopicConsumerConfig<>(TEST_TOPIC_B, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicB.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        // Act
        try {
            consumerRecordProcessor.start();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertEquals(0, counterTopicA.get());
                assertEquals(2, counterTopicB.get());
                assertEquals(
                        2, consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).size());
                assertEquals(
                        1, consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).size());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
            });
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    @Test
    public void should_not_block_processing_when_null_keys() {
        // Arrange
        LockProvider lockProvider = KafkaConsumerRecordProcessorIntegrationTest::lock;

        var counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, null, "value1"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, null, "value2"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 3, null, "value3"));

        // Act
        try {
            consumerRecordProcessor.start();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertEquals(3, counterTopicA.get());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
            });
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    @Test
    public void should_consume_records_if_backoff_expired() {
        // Arrange
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});
        var counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .withBackoffStrategy((r) -> Duration.ofSeconds(-1))
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        List<StoredConsumerRecord> records = consumerRepository.getRecords(TEST_TOPIC_A, 1, 5);
        records.forEach(record -> consumerRepository.incrementRetries(record.getId()));

        // Act
        try {
            consumerRecordProcessor.start();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertEquals(2, counterTopicA.get());
                assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
            });
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    @Test
    public void should_not_consume_records_if_backoff_not_expired() throws InterruptedException {
        // Arrange
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});
        var counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(TEST_TOPIC_A, stringDeserializer(), stringDeserializer(), (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                }));

        var consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder.builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(configs)
                .withBackoffStrategy((r) -> Duration.ofHours(1))
                .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

        List<StoredConsumerRecord> records = consumerRepository.getRecords(TEST_TOPIC_A, 1, 5);
        records.forEach(record -> consumerRepository.incrementRetries(record.getId()));

        // Act
        try {
            consumerRecordProcessor.start();
            Thread.sleep(1000);
            assertEquals(0, counterTopicA.get());
            assertEquals(2, consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).size());
        } finally {
            consumerRecordProcessor.stop();
        }
    }

    private StoredConsumerRecord storedRecord(String topic, int partition, long offset, String key, String value) {
        return new StoredConsumerRecord(
                topic,
                partition,
                offset,
                key != null ? key.getBytes() : null,
                value.getBytes(),
                "[]",
                System.currentTimeMillis());
    }
}
