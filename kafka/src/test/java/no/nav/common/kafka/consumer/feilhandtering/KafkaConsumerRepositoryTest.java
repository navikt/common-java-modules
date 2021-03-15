package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.utils.LocalH2Database;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class KafkaConsumerRepositoryTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        DataSource postgres = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.init(postgres, "kafka-record-postgres.sql");
        PostgresConsumerRepository<String, String> postgresConsumerRepository = new PostgresConsumerRepository<>(
                postgres,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );

        DataSource oracle = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.ORACLE);
        LocalH2Database.init(oracle, "kafka-record-oracle.sql");
        OracleConsumerRepository<String, String> oracleConsumerRepository = new OracleConsumerRepository<>(
                oracle,
                new StringSerializer(),
                new StringDeserializer(),
                new StringSerializer(),
                new StringDeserializer()
        );

        return Arrays.asList(
                new Object[]{LocalH2Database.DatabaseType.POSTGRES, postgres, postgresConsumerRepository},
                new Object[]{LocalH2Database.DatabaseType.ORACLE, oracle, oracleConsumerRepository}
        );
    }

    private final DataSource dataSource;

    private final KafkaConsumerRepository<String, String> kafkaConsumerRepository;

    // databaseType must be sent as a parameter for the name to show up when running the tests
    public KafkaConsumerRepositoryTest(LocalH2Database.DatabaseType databaseType, DataSource dataSource, KafkaConsumerRepository<String, String> kafkaConsumerRepository) {
        this.dataSource = dataSource;
        this.kafkaConsumerRepository = kafkaConsumerRepository;
    }

    @After
    public void cleanup() {
        LocalH2Database.cleanup(dataSource);
    }

    @Test
    public void should_insert_consumer_record() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        long id = kafkaConsumerRepository.storeRecord(record);
        assertEquals(1, id);
    }

    @Test
    public void should_not_insert_more_than_1_record_with_same_topic_partition_offset() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        long id1 = kafkaConsumerRepository.storeRecord(record);
        long id2 = kafkaConsumerRepository.storeRecord(record);

        assertEquals(1, id1);
        assertEquals(1, id2);
    }

    @Test
    public void should_retrieve_record() {
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value"));

        KafkaConsumerRecord<String, String> record = kafkaConsumerRepository.getRecords(
                List.of("topic1"),
                10
        ).get(0);

        assertEquals(1, record.getId());
        assertEquals("topic1", record.getTopic());
        assertEquals(1, record.getPartition());
        assertEquals(2, record.getOffset());
        assertEquals("key", record.getKey());
        assertEquals("value", record.getValue());
    }

    @Test
    public void should_retrieve_records_from_topic() {
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value"));

        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic2", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic2", 1, 2, "key", "value"));

        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic3", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic3", 1, 2, "key", "value"));

        List<String> topics = List.of("topic1", "topic3");

        List<KafkaConsumerRecord<String, String>> records = kafkaConsumerRepository.getRecords(
                topics,
                10
        );

        assertEquals(4, records.size());
        records.forEach(record -> {
            assertTrue(topics.contains(record.getTopic()));
        });
    }

    @Test
    public void should_retrieve_records_in_order() {
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value"));

        List<KafkaConsumerRecord<String, String>> records = kafkaConsumerRepository.getRecords(
                List.of("topic1"),
                10
        );

        List<KafkaConsumerRecord<String, String>> sortedRecords = records
                .stream()
                .sorted((r1, r2) -> (int) (r1.getId() - r2.getId())) // Sort id ascending
                .collect(Collectors.toList());

        for (int i = 0; i < records.size(); i++) {
            assertEquals(records.get(i), sortedRecords.get(i));
        }
    }

    @Test
    public void should_retrieve_records_with_limit() {
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value"));

        List<KafkaConsumerRecord<String, String>> records = kafkaConsumerRepository.getRecords(
                List.of("topic1"),
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_delete_record() {
        long id = kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value"));
        kafkaConsumerRepository.storeRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value"));

        kafkaConsumerRepository.deleteRecord(id);

        List<KafkaConsumerRecord<String, String>> records = kafkaConsumerRepository.getRecords(
                List.of("topic1"),
                10
        );

        assertEquals(1, records.size());
    }


}
