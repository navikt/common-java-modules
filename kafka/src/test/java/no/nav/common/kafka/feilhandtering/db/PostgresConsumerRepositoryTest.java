package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaConsumerRecord;
import no.nav.common.kafka.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostgresConsumerRepositoryTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:11.5");

    private PostgresConsumerRepository<String, String> postgresConsumerRepository;

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

        postgresConsumerRepository = new PostgresConsumerRepository<>(
                dataSource,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );
    }

    @Test
    public void should_insert_producer_record() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key","value");
        long id = postgresConsumerRepository.storeRecord(record);
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key","value"));

        KafkaConsumerRecord<String, String> record = postgresConsumerRepository.getRecords(
                List.of("topic1"),
                10
        ).get(0);

        assertEquals(1, record.id);
        assertEquals("topic1", record.topic);
        assertEquals(1, record.partition);
        assertEquals(2, record.offset);
        assertEquals("key", record.key);
        assertEquals("value", record.value);
    }

    @Test
    public void should_retrieve_records_from_topic() {
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key","value"));

        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic2", 1, 1, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic2", 1, 2, "key","value"));

        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic3", 1, 1, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic3", 1, 2, "key","value"));

        List<String> topics = List.of("topic1", "topic3");

        List<KafkaConsumerRecord<String, String>> records = postgresConsumerRepository.getRecords(
                topics,
                10
        );

        assertEquals(4, records.size());
        records.forEach(record -> {
            assertTrue(topics.contains(record.topic));
        });
    }

    @Test
    public void should_retrieve_records_with_limit() {
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 3, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 4, "key","value"));

        List<KafkaConsumerRecord<String, String>> records = postgresConsumerRepository.getRecords(
                List.of("topic1"),
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_record() {
        long id = postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key","value"));
        postgresConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key","value"));

        postgresConsumerRepository.deleteRecord(id);

        List<KafkaConsumerRecord<String, String>> records = postgresConsumerRepository.getRecords(
                List.of("topic1"),
                10
        );

        assertEquals(1, records.size());
    }
    
}
