package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.utils.LocalH2Database;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
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

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class KafkaConsumerRepositoryTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        DataSource postgres = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.init(postgres, "kafka-consumer-record-postgres.sql");
        PostgresConsumerRepository postgresConsumerRepository = new PostgresConsumerRepository(postgres);

        DataSource oracle = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.ORACLE);
        LocalH2Database.init(oracle, "kafka-consumer-record-oracle.sql");
        OracleConsumerRepository oracleConsumerRepository = new OracleConsumerRepository(oracle);

        return Arrays.asList(
                new Object[]{LocalH2Database.DatabaseType.POSTGRES, postgres, postgresConsumerRepository},
                new Object[]{LocalH2Database.DatabaseType.ORACLE, oracle, oracleConsumerRepository}
        );
    }

    private final DataSource dataSource;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    // databaseType must be sent as a parameter for the name to show up when running the tests
    public KafkaConsumerRepositoryTest(LocalH2Database.DatabaseType databaseType, DataSource dataSource, KafkaConsumerRepository kafkaConsumerRepository) {
        this.dataSource = dataSource;
        this.kafkaConsumerRepository = kafkaConsumerRepository;
    }

    @After
    public void cleanup() {
        LocalH2Database.cleanupConsumer(dataSource);
    }

    @Test
    public void should_insert_consumer_record() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        long id = kafkaConsumerRepository.storeRecord(mapRecord(record));
        assertEquals(1, id);
    }

    @Test
    public void should_not_insert_more_than_1_record_with_same_topic_partition_offset() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");
        long id1 = kafkaConsumerRepository.storeRecord(mapRecord(record));
        long id2 = kafkaConsumerRepository.storeRecord(mapRecord(record));

        assertEquals(1, id1);
        assertEquals(-1, id2);
    }

    @Test
    public void should_retrieve_record() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic1", 1, 2, "key", "value");
        consumerRecord.headers().add(new RecordHeader("header1", "test".getBytes()));

        kafkaConsumerRepository.storeRecord(mapRecord(consumerRecord));

        KafkaConsumerRecord record = kafkaConsumerRepository.getRecords(
                "topic1",
                1,
                5
        ).get(0);

        assertEquals(1, record.getId());
        assertEquals("topic1", record.getTopic());
        assertEquals(1, record.getPartition());
        assertEquals(2, record.getOffset());
        assertArrayEquals("key".getBytes(), record.getKey());
        assertArrayEquals("value".getBytes(), record.getValue());
        assertEquals("[{\"key\":\"header1\",\"value\":\"dGVzdA==\"}]", record.getHeadersJson());
    }

    @Test
    public void should_retrieve_records_in_order() {
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value")));

        List<KafkaConsumerRecord> records = kafkaConsumerRepository.getRecords(
                "topic1",
                1,
                5
        );

        List<KafkaConsumerRecord> sortedRecords = records
                .stream()
                .sorted((r1, r2) -> (int) (r1.getId() - r2.getId())) // Sort id ascending
                .collect(Collectors.toList());

        for (int i = 0; i < records.size(); i++) {
            assertEquals(records.get(i), sortedRecords.get(i));
        }
    }

    @Test
    public void should_retrieve_records_with_limit() {
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 4, "key", "value")));

        List<KafkaConsumerRecord> records = kafkaConsumerRepository.getRecords(
                "topic1",
                1,
                3
        );

        assertEquals(3, records.size());
    }

    @Test
    public void should_find_record_with_key() {
        String key2 = "key2";
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, key2, "value")));
        assertTrue(kafkaConsumerRepository.hasRecordWithKey("topic1", 1, key2.getBytes()));
    }

    @Test
    public void should_not_find_record_with_different_key() {
        String key2 = "key2";
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key1", "value")));
        assertFalse(kafkaConsumerRepository.hasRecordWithKey("topic1", 1, key2.getBytes()));
    }

    @Test
    public void should_increment_retries() {
        long id = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key1", "value")));

        kafkaConsumerRepository.incrementRetries(id);
        kafkaConsumerRepository.incrementRetries(id);

        List<KafkaConsumerRecord> records = kafkaConsumerRepository.getRecords(
                "topic1",
                1,
                3
        );

        assertEquals(2, records.get(0).getRetries());
        assertNotNull(records.get(0).getLastRetry());
    }

    @Test
    public void should_get_topic_partitions() {
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key1", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 2, 1, "key1", "value")));
        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 2, 2, "key1", "value")));

        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic2", 1, 1, "key1", "value")));

        kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic3", 1, 1, "key1", "value")));

        List<TopicPartition> partitions = kafkaConsumerRepository.getTopicPartitions(List.of("topic1", "topic3"));

        assertEquals(3, partitions.size());
        assertEquals(new TopicPartition("topic1", 1), partitions.get(0));
        assertEquals(new TopicPartition("topic1", 2), partitions.get(1));
        assertEquals(new TopicPartition("topic3", 1), partitions.get(2));
    }

    @Test
    public void should_delete_records() {
        long id1 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 1, "key", "value")));
        long id2 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 2, "key", "value")));
        long id3 = kafkaConsumerRepository.storeRecord(mapRecord(new ConsumerRecord<>("topic1", 1, 3, "key", "value")));

        kafkaConsumerRepository.deleteRecords(List.of(id1, id3));

        List<KafkaConsumerRecord> records = kafkaConsumerRepository.getRecords(
                "topic1",
                1,
                5
        );

        assertEquals(1, records.size());
        assertEquals(id2, records.get(0).getId());
    }

    private KafkaConsumerRecord mapRecord(ConsumerRecord<String, String> record) {
        return ConsumerUtils.mapToStoredRecord(record, new StringSerializer(), new StringSerializer());
    }

}
