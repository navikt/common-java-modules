package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.util.ProducerUtils;
import no.nav.common.kafka.utils.LocalH2Database;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
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
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class KafkaProducerRepositoryTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        DataSource postgres = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.init(postgres, "kafka-producer-record-postgres.sql");
        PostgresProducerRepository postgresProducerRepository = new PostgresProducerRepository(postgres);

        DataSource oracle = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.ORACLE);
        LocalH2Database.init(oracle, "kafka-producer-record-oracle.sql");
        OracleProducerRepository oracleProducerRepository = new OracleProducerRepository(oracle);

        return Arrays.asList(
                new Object[]{LocalH2Database.DatabaseType.POSTGRES, postgres, postgresProducerRepository},
                new Object[]{LocalH2Database.DatabaseType.ORACLE, oracle, oracleProducerRepository}
        );
    }

    private final DataSource dataSource;

    private final KafkaProducerRepository kafkaProducerRepository;

    // databaseType must be sent as a parameter for the name to show up when running the tests
    public KafkaProducerRepositoryTest(LocalH2Database.DatabaseType databaseType, DataSource dataSource, KafkaProducerRepository kafkaProducerRepository) {
        this.dataSource = dataSource;
        this.kafkaProducerRepository = kafkaProducerRepository;
    }

    @After
    public void cleanup() {
        LocalH2Database.cleanupProducer(dataSource);
    }

    @Test
    public void should_insert_producer_record() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key","value");
        long id = kafkaProducerRepository.storeRecord(mapRecord(record));
        assertEquals(1, id);
    }

    @Test
    public void should_retrieve_record() {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("topic1", 1, "key","value");
        producerRecord.headers().add(new RecordHeader("header1", "test".getBytes()));

        kafkaProducerRepository.storeRecord(mapRecord(producerRecord));

        KafkaProducerRecord record = kafkaProducerRepository.getRecords(
                Instant.now().minusSeconds(10),
                10
        ).get(0);

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
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        List<KafkaProducerRecord> records = kafkaProducerRepository.getRecords(
                Instant.now().minusSeconds(10),
                10
        );

        List<KafkaProducerRecord> sortedRecords = records
                .stream()
                .sorted((r1, r2) -> (int) (r1.getId() - r2.getId())) // Sort id ascending
                .collect(Collectors.toList());

        for (int i = 0; i < records.size(); i++) {
            assertEquals(records.get(i), sortedRecords.get(i));
        }
    }

    @Test
    public void should_retrieve_records_older_than() throws InterruptedException {
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        Thread.sleep(3000);

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        List<KafkaProducerRecord> records = kafkaProducerRepository.getRecords(
                Instant.now().minusMillis(3000),
                10
        );

        assertEquals(2, records.size());
    }

    @Test
    public void should_retrieve_records_with_limit() {
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        List<KafkaProducerRecord> records = kafkaProducerRepository.getRecords(
                Instant.now().minusSeconds(10),
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_record() {
        long id = kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));
        kafkaProducerRepository.storeRecord(mapRecord(new ProducerRecord<>("topic1", "key","value")));

        kafkaProducerRepository.deleteRecord(id);

        List<KafkaProducerRecord> records = kafkaProducerRepository.getRecords(
                Instant.now().minusSeconds(10),
                10
        );

        assertEquals(1, records.size());
    }

    private static KafkaProducerRecord mapRecord(ProducerRecord<String, String> record) {
        return ProducerUtils.mapToStoredRecord(record, new StringSerializer(), new StringSerializer());
    }

}
