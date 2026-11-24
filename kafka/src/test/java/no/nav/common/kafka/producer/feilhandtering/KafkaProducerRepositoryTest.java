package no.nav.common.kafka.producer.feilhandtering;

import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresDataSource;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresTestContainer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import no.nav.common.kafka.producer.util.ProducerUtils;
import no.nav.common.kafka.spring.OracleJdbcTemplateProducerRepository;
import no.nav.common.kafka.spring.PostgresJdbcTemplateProducerRepository;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ParameterizedClass(name = "{0}")
@MethodSource("data")
public class KafkaProducerRepositoryTest {

    public static final PostgreSQLContainer postgreSQLContainer = createPostgresTestContainer();
    private static HikariDataSource postgresDataSource;

    public static Collection<Object[]> data() throws Exception {
        postgreSQLContainer.start();
        postgresDataSource = createPostgresDataSource(postgreSQLContainer);
        DataSource postgres = postgresDataSource;

        DbUtils.runScript(postgres, "kafka-producer-record-postgres.sql");
        var postgresProducerRepository = new PostgresJdbcTemplateProducerRepository(new JdbcTemplate(postgres));

        DataSource oracle = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(oracle, "kafka-producer-record-oracle.sql");
        var oracleProducerRepository = new OracleJdbcTemplateProducerRepository(new JdbcTemplate(oracle));

        return Arrays.asList(
                new Object[] {"POSTGRES", postgres, postgresProducerRepository},
                new Object[] {"ORACLE", oracle, oracleProducerRepository});
    }

    @AfterAll
    public static void stop() {
        try {
            if (postgresDataSource != null) {
                postgresDataSource.close();
            }
        } finally {
            postgreSQLContainer.stop();
        }
    }

    private final DataSource dataSource;
    private final KafkaProducerRepository kafkaProducerRepository;

    // JUnit sender databaseetiketten som et argument, selv om den bare brukes i testnavnet.
    @SuppressWarnings("java:S1172")
    public KafkaProducerRepositoryTest(
            @SuppressWarnings("unused") String databaseType,
            DataSource dataSource,
            KafkaProducerRepository kafkaProducerRepository) {
        this.dataSource = dataSource;
        this.kafkaProducerRepository = kafkaProducerRepository;
    }

    @AfterEach
    public void cleanup() throws Exception {
        DbUtils.cleanupProducer(dataSource);
    }

    @Test
    public void should_insert_producer_record() {
        // Arrange
        var producerRecord = new ProducerRecord<>("topic", "key", "value");
        // Act
        long id = kafkaProducerRepository.storeRecord(mapRecord(producerRecord));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_insert_producer_record_with_null_value() {
        // Arrange
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", null);
        // Act
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_insert_producer_record_with_null_key() {
        // Arrange
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", null, "value");
        // Act
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        // Assert
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        // Arrange
        var producerRecord = new ProducerRecord<>("topic1", 1, "key", "value");
        producerRecord.headers().add(new RecordHeader("header1", "test".getBytes()));

        kafkaProducerRepository.storeRecord(mapRecord(producerRecord));

        // Act
        StoredProducerRecord storedRecord =
                kafkaProducerRepository.getRecords(10).getFirst();

        // Assert
        assertEquals(1, storedRecord.getId());
        assertEquals("topic1", storedRecord.getTopic());
        assertArrayEquals("key".getBytes(), storedRecord.getKey());
        assertArrayEquals("value".getBytes(), storedRecord.getValue());
        assertEquals("[{\"key\":\"header1\",\"value\":\"dGVzdA==\"}]", storedRecord.getHeadersJson());
    }

    @Test
    public void should_retrieve_records_in_order() {
        // Arrange
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", null, "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));

        // Act
        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(10);

        // Assert
        assertEquals(5, records.size());
        assertEquals(
                List.of(1L, 2L, 3L, 4L, 5L),
                records.stream().map(StoredProducerRecord::getId).toList());
    }

    @Test
    public void should_retrieve_records_with_limit() {
        // Arrange
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));

        // Act
        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(3);

        // Assert
        assertEquals(3, records.size());
    }

    @Test
    public void should_retrieve_records_with_topic() {
        // Arrange
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key", "value")));

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key", null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic3", "key", "value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic4", "key", "value")));

        // Act
        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(5, List.of("topic1", "topic3"));

        // Assert
        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_records() {
        // Arrange
        long id1 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key1", "value1")));
        long id2 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key2", "value2")));
        long id3 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key3", "value3")));
        long id4 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key3", null)));
        long id5 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", null, "value5")));

        // Act
        kafkaProducerRepository.deleteRecords(List.of(id1, id3, id4, id5));

        // Assert
        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(10);

        assertEquals(1, records.size());
        assertEquals(id2, records.getFirst().getId());
    }

    private static StoredProducerRecord mapRecord(ProducerRecord<String, String> record) {
        return ProducerUtils.mapToStoredRecord(record, new StringSerializer(), new StringSerializer());
    }
}
