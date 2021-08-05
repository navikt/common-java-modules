package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.util.ProducerUtils;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresContainer;
import static no.nav.common.kafka.utils.LocalPostgresDatabase.createPostgresDataSource;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class KafkaProducerRepositoryTest {

    public static final PostgreSQLContainer<?> postgreSQLContainer = createPostgresContainer();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        postgreSQLContainer.start();
        DataSource postgres = createPostgresDataSource(postgreSQLContainer);

        DbUtils.runScript(postgres, "kafka-producer-record-postgres.sql");
        PostgresProducerRepository postgresProducerRepository = new PostgresProducerRepository(new JdbcTemplate(postgres));

        DataSource oracle = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(oracle, "kafka-producer-record-oracle.sql");
        OracleProducerRepository oracleProducerRepository = new OracleProducerRepository(new JdbcTemplate(oracle));

        return Arrays.asList(
                new Object[]{"POSTGRES", postgres, postgresProducerRepository},
                new Object[]{"ORACLE", oracle, oracleProducerRepository}
        );
    }

    @AfterClass
    public static void stop() {
        postgreSQLContainer.stop();
    }

    private final DataSource dataSource;

    private final KafkaProducerRepository kafkaProducerRepository;

    // databaseType must be sent as a parameter for the name to show up when running the tests
    public KafkaProducerRepositoryTest(String databaseType, DataSource dataSource, KafkaProducerRepository kafkaProducerRepository) {
        this.dataSource = dataSource;
        this.kafkaProducerRepository = kafkaProducerRepository;
    }

    @After
    public void cleanup() {
        DbUtils.cleanupProducer(dataSource);
    }

    @Test
    public void should_insert_producer_record() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key","value");
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        assertEquals(1, id);
    }

    @Test
    public void should_insert_producer_record_with_null_value() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key",null);
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        assertEquals(1, id);
    }

    @Test
    public void should_insert_producer_record_with_null_key() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", null,"value");
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("topic1", 1, "key","value");
        producerRecord.headers().add(new RecordHeader("header1", "test".getBytes()));

        kafkaProducerRepository.storeRecord(mapRecord(producerRecord));

        StoredProducerRecord record = kafkaProducerRepository.getRecords(10).get(0);

        assertEquals(1, record.getId());
        assertEquals("topic1", record.getTopic());
        assertArrayEquals("key".getBytes(), record.getKey());
        assertArrayEquals("value".getBytes(), record.getValue());
        assertEquals("[{\"key\":\"header1\",\"value\":\"dGVzdA==\"}]", record.getHeadersJson());
    }

    @Test
    public void should_retrieve_records_in_order() {
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key",null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", null,"value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(10);

        List<StoredProducerRecord> sortedRecords = records
                .stream()
                .sorted((r1, r2) -> (int) (r1.getId() - r2.getId())) // Sort id ascending
                .collect(Collectors.toList());

        for (int i = 0; i < records.size(); i++) {
            assertEquals(records.get(i), sortedRecords.get(i));
        }
    }

    @Test
    public void should_retrieve_records_with_limit() {
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key",null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(3);

        assertEquals(3, records.size());
    }

    @Test
    public void should_retrieve_records_with_topic() {
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key",null)));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic3", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic4", "key","value")));

        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(5, List.of("topic1", "topic3"));

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_records() {
        long id1 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key1","value1")));
        long id2 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key2","value2")));
        long id3 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key3","value3")));
        long id4 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", "key3",null)));
        long id5 = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic2", null,"value5")));

        kafkaProducerRepository.deleteRecords(List.of(id1, id3, id4, id5));

        List<StoredProducerRecord> records = kafkaProducerRepository.getRecords(10);

        assertEquals(1, records.size());
        assertEquals(id2, records.get(0).getId());
    }

    private static StoredProducerRecord mapRecord(ProducerRecord<String, String> record) {
        return ProducerUtils.mapToStoredRecord(record, new StringSerializer(), new StringSerializer());
    }

}
