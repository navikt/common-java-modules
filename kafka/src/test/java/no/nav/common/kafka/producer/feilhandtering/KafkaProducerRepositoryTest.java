package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.utils.LocalH2Database;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class KafkaProducerRepositoryTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        DataSource postgres = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.init(postgres, "kafka-record-postgres.sql");
        PostgresProducerRepository<String, String> postgresProducerRepository = new PostgresProducerRepository<>(
                postgres,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );

        DataSource oracle = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.ORACLE);
        LocalH2Database.init(oracle, "kafka-record-oracle.sql");
        OracleProducerRepository<String, String> oracleProducerRepository = new OracleProducerRepository<>(
                oracle,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );

        return Arrays.asList(
                new Object[]{LocalH2Database.DatabaseType.POSTGRES, postgres, postgresProducerRepository},
                new Object[]{LocalH2Database.DatabaseType.ORACLE, oracle, oracleProducerRepository}
        );
    }

    private final DataSource dataSource;

    private final KafkaProducerRepository<String, String> kafkaProducerRepository;

    // databaseType must be sent as a parameter for the name to show up when running the tests
    public KafkaProducerRepositoryTest(LocalH2Database.DatabaseType databaseType, DataSource dataSource, KafkaProducerRepository<String, String> kafkaProducerRepository) {
        this.dataSource = dataSource;
        this.kafkaProducerRepository = kafkaProducerRepository;
    }

    @After
    public void cleanup() {
        LocalH2Database.cleanup(dataSource);
    }

    @Test
    public void should_insert_producer_record() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key","value");
        long id = kafkaProducerRepository.storeRecord(record);
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        KafkaProducerRecord<String, String> record = kafkaProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusSeconds(10),
                10
        ).get(0);

        assertEquals(1, record.id);
        assertEquals("topic1", record.topic);
        assertEquals("key", record.key);
        assertEquals("value", record.value);
    }

    @Test
    public void should_retrieve_records_from_topic() {
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic2", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic2", "key","value"));

        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic3", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic3", "key","value"));

        List<String> topics = List.of("topic1", "topic3");

        List<KafkaProducerRecord<String, String>> records = kafkaProducerRepository.getRecords(
                topics,
                Instant.now().minusSeconds(10),
                10
        );

        assertEquals(4, records.size());
        records.forEach(record -> {
            assertTrue(topics.contains(record.topic));
        });
    }

    @Test
    public void should_retrieve_records_older_than() throws InterruptedException {
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        Thread.sleep(3000);

        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        List<KafkaProducerRecord<String, String>> records = kafkaProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusMillis(3000),
                10
        );

        assertEquals(2, records.size());
    }

    @Test
    public void should_retrieve_records_with_limit() {
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        List<KafkaProducerRecord<String, String>> records = kafkaProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusSeconds(10),
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_record() {
        long id = kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        kafkaProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        kafkaProducerRepository.deleteRecord(id);

        List<KafkaProducerRecord<String, String>> records = kafkaProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusSeconds(10),
                10
        );

        assertEquals(1, records.size());
    }

}
