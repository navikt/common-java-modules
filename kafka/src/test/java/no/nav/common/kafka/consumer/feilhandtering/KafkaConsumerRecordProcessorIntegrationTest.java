package no.nav.common.kafka.consumer.feilhandtering;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.utils.DbUtils;
import no.nav.common.kafka.utils.LocalOracleH2Database;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaConsumerRecordProcessorIntegrationTest {

    private final static String TEST_TOPIC_A = "test-topic-a";

    private final static String TEST_TOPIC_B = "test-topic-b";

    private DataSource dataSource;

    private KafkaConsumerRepository consumerRepository;

    private static Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        return Optional.of((SimpleLock) () -> {
        });
    }

    @Before
    public void setup() {
        dataSource = LocalOracleH2Database.createDatabase();
        DbUtils.runScript(dataSource, "kafka-consumer-record-postgres.sql");
        consumerRepository = new PostgresConsumerRepository(dataSource);
    }

    @After
    public void cleanup() {
        DbUtils.cleanupConsumer(dataSource);
    }

    @Test
    public void should_consume_stored_records() throws InterruptedException {
        LockProvider lockProvider = lockConfiguration -> Optional.of(() -> {
        });

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                ),
                new TopicConsumerConfig<>(
                        TEST_TOPIC_B,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicB.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

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
                return Optional.of((SimpleLock) () -> {
                });
            }

            return Optional.empty();
        };

        AtomicInteger counterTopicA = new AtomicInteger();
        AtomicInteger counterTopicB = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                ),
                new TopicConsumerConfig<>(
                        TEST_TOPIC_B,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            counterTopicB.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, "key1", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 2, 1, "key2", "value"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, "key1", "value"));

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

    @Test
    public void should_not_block_processing_when_null_keys() throws InterruptedException {
        LockProvider lockProvider = KafkaConsumerRecordProcessorIntegrationTest::lock;

        AtomicInteger counterTopicA = new AtomicInteger();

        List<TopicConsumerConfig<?, ?>> configs = List.of(
                new TopicConsumerConfig<>(
                        TEST_TOPIC_A,
                        stringDeserializer(),
                        stringDeserializer(),
                        (record) -> {
                            if (record.offset() == 2) {
                                return ConsumeStatus.FAILED;
                            }

                            counterTopicA.incrementAndGet();
                            return ConsumeStatus.OK;
                        }
                )
        );

        KafkaConsumerRecordProcessor consumerRecordProcessor =
                KafkaConsumerRecordProcessorBuilder
                        .builder()
                        .withLockProvider(lockProvider)
                        .withKafkaConsumerRepository(consumerRepository)
                        .withConsumerConfigs(configs)
                        .build();

        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 1, null, "value1"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 2, null, "value2"));
        consumerRepository.storeRecord(storedRecord(TEST_TOPIC_A, 1, 3, null, "value3"));

        consumerRecordProcessor.start();
        Thread.sleep(1000);
        consumerRecordProcessor.close();

        assertEquals(2, counterTopicA.get());

    }

    private StoredConsumerRecord storedRecord(String topic, int partition, long offset, String key, String value) {
        return new StoredConsumerRecord(
                topic,
                partition,
                offset,
                key != null ? key.getBytes() : null,
                value.getBytes(),
                "[]",
                System.currentTimeMillis()
        );
    }

}
