package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaProducerRecord;
import no.nav.common.kafka.utils.TestUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostgresProducerRepositoryTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:11.5");

    private PostgresProducerRepository<String, String> postgresProducerRepository;

    private PGSimpleDataSource dataSource;

    @Before
    public void setup() throws SQLException {
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(container.getJdbcUrl());
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());

        try(Statement statement = dataSource.getConnection().createStatement()) {
            String postgresSql = TestUtils.readTestResourceFile("kafka-record-postgres.sql");
            statement.execute(postgresSql);
        }

        postgresProducerRepository = new PostgresProducerRepository<>(
                dataSource,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );
    }

    @Test
    public void should_insert_producer_record() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key","value");
        long id = postgresProducerRepository.storeRecord(record);
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        KafkaProducerRecord<String, String> record = postgresProducerRepository.getRecords(
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
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic2", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic2", "key","value"));

        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic3", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic3", "key","value"));

        List<String> topics = List.of("topic1", "topic3");

        List<KafkaProducerRecord<String, String>> records = postgresProducerRepository.getRecords(
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
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        Thread.sleep(3000);

        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        List<KafkaProducerRecord<String, String>> records = postgresProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusMillis(3000),
                10
        );

        assertEquals(2, records.size());
    }

    @Test
    public void should_retrieve_records_with_limit() {
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        List<KafkaProducerRecord<String, String>> records = postgresProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusSeconds(10),
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_record() {
        long id = postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));
        postgresProducerRepository.storeRecord(new ProducerRecord<>("topic1", "key","value"));

        postgresProducerRepository.deleteRecord(id);

        List<KafkaProducerRecord<String, String>> records = postgresProducerRepository.getRecords(
                List.of("topic1"),
                Instant.now().minusSeconds(10),
                10
        );

        assertEquals(1, records.size());
    }

}
