package no.nav.common.kafka.consumer.feilhandtering;

import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresDataSource;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresTestContainer;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.spring.OracleJdbcTemplateConsumerRepository;
import no.nav.common.kafka.spring.PostgresJdbcTemplateConsumerRepository;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@RunWith(Parameterized.class)
public class KafkaConsumerRepositoryTest {

    public static final PostgreSQLContainer postgreSQLContainer = createPostgresTestContainer();

    private static HikariDataSource postgresDataSource;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
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

    private final DataSource dataSource;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    // JUnit sender databaseetiketten som et argument, selv om den bare brukes i testnavnet.
    @SuppressWarnings("java:S1172")
    public KafkaConsumerRepositoryTest(
            @SuppressWarnings("unused") String databaseType,
            DataSource dataSource,
            KafkaConsumerRepository kafkaConsumerRepository) {
        this.dataSource = dataSource;
        this.kafkaConsumerRepository = kafkaConsumerRepository;
    }

    @AfterClass
    public static void stop() {
        try {
            if (postgresDataSource != null) {
                postgresDataSource.close();
            }
        } finally {
            postgreSQLContainer.stop();
        }
    }

    @After
    public void cleanup() {
        DbUtils.cleanupConsumer(dataSource);
    }

    @Test
    public void should_insert_consumer_record() {
        // Arrange
        var consumerRecord = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        // Act
        long id = kafkaConsumerRepository.storeRecord(mapRecord(consumerRecord));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_insert_consumer_record_with_null_value() {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", null);
        // Act
        long id = kafkaConsumerRepository.storeRecord(mapRecord(record));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_insert_consumer_record_with_null_key() {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, null, "value");
        // Act
        long id = kafkaConsumerRepository.storeRecord(mapRecord(record));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_not_insert_more_than_1_record_with_same_topic_partition_offset() {
        // Arrange
        var consumerRecord = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        // Act
        long id1 = kafkaConsumerRepository.storeRecord(mapRecord(consumerRecord));
        long id2 = kafkaConsumerRepository.storeRecord(mapRecord(consumerRecord));

        // Assert
        assertEquals(1, id1);
        assertEquals(-1, id2);
    }

    @Test
    public void should_retrieve_record() {
        // Arrange
        long now = System.currentTimeMillis();
        var headers = new RecordHeaders().add(new RecordHeader("header1", "test".getBytes()));
        Optional<Integer> leaderEpoch = Optional.empty();
        var consumerRecord = new ConsumerRecord<>(
                "topic1",
                1,
                2,
                now,
                TimestampType.CREATE_TIME,
                ConsumerRecord.NULL_SIZE,
                ConsumerRecord.NULL_SIZE,
                "key",
                "value",
                headers,
                leaderEpoch);

        kafkaConsumerRepository.storeRecord(mapRecord(consumerRecord));

        // Act
        StoredConsumerRecord storedRecord =
                kafkaConsumerRepository.getRecords("topic1", 1, 5).getFirst();

        // Assert
        assertEquals(1, storedRecord.getId());
        assertEquals("topic1", storedRecord.getTopic());
        assertEquals(1, storedRecord.getPartition());
        assertEquals(2, storedRecord.getOffset());
        assertArrayEquals("key".getBytes(), storedRecord.getKey());
        assertArrayEquals("value".getBytes(), storedRecord.getValue());
        assertEquals(now, storedRecord.getTimestamp());
        assertEquals("[{\"key\":\"header1\",\"value\":\"dGVzdA==\"}]", storedRecord.getHeadersJson());
    }

    @Test
    public void should_retrieve_records_in_order() {
        // Arrange
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", null)));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));

        // Act
        List<StoredConsumerRecord> records = kafkaConsumerRepository.getRecords("topic1", 1, 5);

        // Assert
        List<StoredConsumerRecord> sortedRecords = records.stream()
                .sorted((r1, r2) -> (int) (r1.getOffset() - r2.getOffset())) // Sort offset ascending
                .toList();

        for (int i = 0; i < records.size(); i++) {
            assertEquals(records.get(i), sortedRecords.get(i));
        }
    }

    @Test
    public void should_retrieve_records_with_limit() {
        // Arrange
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value")));

        // Act
        List<StoredConsumerRecord> records = kafkaConsumerRepository.getRecords("topic1", 1, 3);

        // Assert
        assertEquals(3, records.size());
    }

    @Test
    public void should_find_record_with_key() {
        // Arrange
        String key2 = "key2";
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, key2, "value")));
        // Act & Assert
        assertTrue(kafkaConsumerRepository.hasRecordWithKey("topic1", 1, key2.getBytes()));
    }

    @Test
    public void should_not_find_record_with_different_key() {
        // Arrange
        String key2 = "key2";
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key1", "value")));
        // Act & Assert
        assertFalse(kafkaConsumerRepository.hasRecordWithKey("topic1", 1, key2.getBytes()));
    }

    @Test
    public void should_increment_retries() {
        // Arrange
        long id = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key1", "value")));

        // Act
        kafkaConsumerRepository.incrementRetries(id);
        kafkaConsumerRepository.incrementRetries(id);

        // Assert
        List<StoredConsumerRecord> records = kafkaConsumerRepository.getRecords("topic1", 1, 3);

        assertEquals(2, records.getFirst().getRetries());
        assertNotNull(records.getFirst().getLastRetry());
    }

    @Test
    public void should_get_topic_partitions() {
        // Arrange
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key1", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 2, 1, "key1", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 2, 2, "key1", "value")));

        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic2", 1, 1, "key1", "value")));

        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic3", 1, 1, "key1", "value")));

        // Act
        List<TopicPartition> partitions = kafkaConsumerRepository.getTopicPartitions(List.of("topic1", "topic3"));

        // Assert
        assertEquals(3, partitions.size());
        assertEquals(new TopicPartition("topic1", 1), partitions.get(0));
        assertEquals(new TopicPartition("topic1", 2), partitions.get(1));
        assertEquals(new TopicPartition("topic3", 1), partitions.get(2));
    }

    @Test
    public void should_delete_records() {
        // Arrange
        long id1 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        long id2 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        long id3 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value")));

        // Act
        kafkaConsumerRepository.deleteRecords(List.of(id1, id3));

        // Assert
        List<StoredConsumerRecord> records = kafkaConsumerRepository.getRecords("topic1", 1, 5);

        assertEquals(1, records.size());
        assertEquals(id2, records.getFirst().getId());
    }

    @Test
    public void should_get_failed_record_counts() {
        // Arrange
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        long id2 =
                kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key2", "value")));
        long id3 =
                kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic2", 1, 3, "key3", "value")));
        long id4 =
                kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic2", 1, 4, "key4", "value")));

        kafkaConsumerRepository.incrementRetries(id2);
        kafkaConsumerRepository.incrementRetries(id2);
        kafkaConsumerRepository.incrementRetries(id3);
        kafkaConsumerRepository.incrementRetries(id4);

        // Act
        var counts = kafkaConsumerRepository.getFailedRecordCounts(List.of("topic1", "topic2"));

        // Assert
        assertEquals(2, counts.size());
        assertEquals(1L, counts.get(new TopicPartition("topic1", 1)).longValue());
        assertEquals(2L, counts.get(new TopicPartition("topic2", 1)).longValue());
    }

    private StoredConsumerRecord mapRecord(ConsumerRecord<String, String> record) {
        return ConsumerUtils.mapToStoredRecord(record, new StringSerializer(), new StringSerializer());
    }
}
