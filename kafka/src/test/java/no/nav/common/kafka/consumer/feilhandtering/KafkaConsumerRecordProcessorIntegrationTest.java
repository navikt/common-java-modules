package no.nav.common.kafka.consumer.feilhandtering;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.utils.LocalH2Database;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerRecordProcessorIntegrationTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    private DataSource dataSource;

    private KafkaConsumerRepository consumerRepository;

    @Before
    public void setup() {
        dataSource = LocalH2Database.createDatabase(LocalH2Database.DatabaseType.POSTGRES);
        LocalH2Database.runScript(dataSource, "kafka-consumer-record-postgres.sql");
        consumerRepository = new PostgresConsumerRepository(dataSource);
    }

    @After
    public void cleanup() {
        LocalH2Database.cleanupConsumer(dataSource);
    }

    @Test
    public void should_consume_stored_records() throws InterruptedException {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {});

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        Map<String, StoredRecordConsumer> storedRecordConsumers = Map.of(
                TEST_TOPIC_A, (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                },
                TEST_TOPIC_B, (record) -> {
                    counterTopicB.incrementAndGet();
                    return ConsumeStatus.OK;
                }
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withRecordConsumers(storedRecordConsumers)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2,"key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        consumerRecordProcessor.start();
        Thread.sleep(1000);
        consumerRecordProcessor.close();

        assertEquals(3, counterTopicA.get());
        assertEquals(2, counterTopicB.get());

        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).isEmpty());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).isEmpty());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
    }

    @Test
    public void should_only_consume_from_topics_when_lock_is_acquired() throws InterruptedException {
        LockProvider lockProvider = lockConfiguration -> {
            if (lockConfiguration.getName().contains(TEST_TOPIC_B)) {
                return Optional.of((SimpleLock) () -> {});
            }

            return Optional.empty();
        };

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        Map<String, StoredRecordConsumer> storedRecordConsumers = Map.of(
                TEST_TOPIC_A, (record) -> {
                    counterTopicA.incrementAndGet();
                    return ConsumeStatus.OK;
                },
                TEST_TOPIC_B, (record) -> {
                    counterTopicB.incrementAndGet();
                    return ConsumeStatus.OK;
                }
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withRecordConsumers(storedRecordConsumers)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2,"key1", "value"));

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_B, 1, 2, "key2", "value"));

        consumerRecordProcessor.start();
        Thread.sleep(1000);
        consumerRecordProcessor.close();

        assertEquals(0, counterTopicA.get());
        assertEquals(2, counterTopicB.get());

        assertEquals(2, consumerRepository.getRecords(TEST_TOPIC_A, 1, 5).size());
        assertEquals(1, consumerRepository.getRecords(TEST_TOPIC_A, 2, 5).size());
        assertTrue(consumerRepository.getRecords(TEST_TOPIC_B, 1, 5).isEmpty());
    }

    private StoredConsumerRecord storedRecord(String topic, int partition, long offset, String key, String value) {
        return new StoredConsumerRecord(
                topic,
                partition,
                offset,
                key.getBytes(),
                value.getBytes(),
                "[]",
                System.currentTimeMillis()
        );
    }

}
