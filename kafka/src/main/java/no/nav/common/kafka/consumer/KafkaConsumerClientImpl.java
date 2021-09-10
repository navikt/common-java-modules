package no.nav.common.kafka.consumer;

import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;

/**
 * An abstracted client which uses {@link KafkaConsumer} to poll records.
 * Each consumer is excepted to return a {@link ConsumeStatus} to indicate whether the record was consumed successfully.
 * If a record fails to be consumed, then no more records for that partition will be consumed until the record is consumed successfully.
 * Consumption on topics is performed on a single thread pr partition to ensure that messages are read in order.
 *
 * @param <K> topic key
 * @param <V> topic value
 */
public class KafkaConsumerClientImpl<K, V> implements KafkaConsumerClient, ConsumerRebalanceListener {

    public final static long DEFAULT_POLL_DURATION_MS = 1000;

    private final static long POLL_ERROR_TIMEOUT_MS = 5000;

    private enum ClientState {
        RUNNING, NOT_RUNNING
    }

    private final Logger log = LoggerFactory.getLogger(KafkaConsumerClientImpl.class);

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    private final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedPartitions = ConcurrentHashMap.newKeySet();

    private final Set<ConsumerRecord<K, V>> failedRecords = ConcurrentHashMap.newKeySet();

    private final KafkaConsumerClientConfig<K, V> config;

    private volatile ClientState clientState = ClientState.NOT_RUNNING;

    private volatile CountDownLatch processedRecordsLatch;

    private volatile CountDownLatch shutdownLatch;

    private KafkaConsumer<K, V> consumer;

    public KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config) {
        validateConfig(config);

        this.config = config;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public void start() {
        if (clientState == ClientState.RUNNING) {
            return;
        }

        clientState = ClientState.RUNNING;

        log.info("Starting kafka consumer client...");

        pollExecutor.submit(this::consumeTopics);
    }

    @Override
    public void stop() {
        if (clientState != ClientState.RUNNING) {
            return;
        }

        clientState = ClientState.NOT_RUNNING;

        log.info("Stopping kafka consumer client...");

        try {
            consumer.wakeup(); // Will abort an ongoing consumer.poll()
            shutdownLatch.await(10, TimeUnit.SECONDS);
            log.info("Kafka consumer client stopped");
        } catch (InterruptedException e) {
            log.error("Failed to stop kafka consumer client gracefully", e);
        }
    }

    @Override
    public boolean isRunning() {
        return clientState == ClientState.RUNNING;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions has been revoked from consumer: " + Arrays.toString(partitions.toArray()));
        revokedPartitions.addAll(partitions);
        try {
            commitCurrentOffsets();
        } catch (Exception e) {
            log.error("Failed to commit offsets when partitions were revoked: " + offsetsToCommit.toString(), e);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("New partitions has been assigned to the consumer: " + Arrays.toString(partitions.toArray()));
    }

    private void consumeTopics() {
        final List<String> topicNames = new ArrayList<>(config.topics.keySet());
        final Map<TopicPartition, ExecutorService> topicConsumptionExecutors = new HashMap<>();

        try {
            shutdownLatch = new CountDownLatch(1);
            consumer = new KafkaConsumer<>(config.properties);
            consumer.subscribe(topicNames, this);

            while (clientState == ClientState.RUNNING) {
                ConsumerRecords<K, V> records;

                revokedPartitions.clear();
                offsetsToCommit.clear();

                try {
                    records = consumer.poll(Duration.ofMillis(config.pollDurationMs));
                } catch (WakeupException e) {
                    log.info("Polling was cancelled by wakeup(). Stopping kafka consumer client...");
                    return;
                } catch (Exception e) {
                    log.error("Exception occurred during polling of records. Waiting before trying again", e);
                    Thread.sleep(POLL_ERROR_TIMEOUT_MS);
                    continue;
                }

                if (records.isEmpty()) {
                    continue;
                }

                int totalRecords = records.count();

                processedRecordsLatch = new CountDownLatch(totalRecords);

                for (ConsumerRecord<K, V> record : records) {
                    String topic = record.topic();

                    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

                    TopicConsumer<K, V> topicConsumer = config.topics.get(topic);

                    // Creates a single thread to consume each topic + partition sequentially
                    ExecutorService executor = topicConsumptionExecutors
                            .computeIfAbsent(topicPartition, (tp) -> Executors.newSingleThreadExecutor());

                    executor.submit(() -> {
                        try {
                            /*
                                If the client is stopped then fast-track through the remaining records so that
                                 we can wait for those that are already being consumed after the while-loop.

                                If some previous record on the same topic + partition has failed to be consumed or has been revoked during rebalancing,
                                 then we cannot consume any more records for this topic + partition for the duration of this poll.

                                 The finally-clause will make sure that the processed records counter is incremented.
                            */
                            if (clientState == ClientState.NOT_RUNNING ||
                                    revokedPartitions.contains(topicPartition) ||
                                    failedRecords.stream().anyMatch(failedRecord  ->
                                            failedRecord.topic().equals(record.topic()) &&
                                                    failedRecord.partition() == record.partition())
                            ) {
                                return;
                            }

                            ConsumeStatus status = ConsumerUtils.safeConsume(topicConsumer, record);

                            if (status == ConsumeStatus.OK) {
                                /*
                                    From KafkaConsumer.commitSync documentation:

                                    The committed offset should be the next message your application will consume, i.e.
                                    lastProcessedMessageOffset + 1.
                                 */
                                OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(record.offset() + 1);
                                offsetsToCommit.put(topicPartition, offsetAndMetadata);
                            } else {
                                failedRecords.add(record);
                            }
                        } catch (Exception e) {
                            String msg = format(
                                    "Unexpected error occurred while processing consumer record. topic=%s partition=%d offset=%d",
                                    topic, record.partition(), record.offset()
                            );
                            log.error(msg, e);
                        } finally {
                            processedRecordsLatch.countDown();
                        }
                    });
                }

                processedRecordsLatch.await();

                try {
                    commitCurrentOffsets();
                    seekBackOnFailed();
                } catch (Exception e) {
                    // If we fail to commit offsets then continue polling records
                    log.error("Failed to commit offsets: " + offsetsToCommit.toString(), e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught from main loop. Shutting down...", e);
        } finally {
            try {
                topicConsumptionExecutors.forEach(((topicPartition, executorService) -> executorService.shutdown()));
                commitCurrentOffsets();
                consumer.close(Duration.ofSeconds(3));
            } catch (Exception e) {
                log.error("Failed to shutdown kafka consumer client properly", e);
            } finally {
                shutdownLatch.countDown();

                if (clientState == ClientState.RUNNING) {
                    log.warn("Unexpected failure while kafka consumer client was running. Restarting...");
                    pollExecutor.submit(this::consumeTopics);
                }
            }
        }
    }

    private void commitCurrentOffsets() {
        if (!offsetsToCommit.isEmpty()) {
            consumer.commitSync(offsetsToCommit, Duration.ofSeconds(3));
            log.info("Offsets committed: " + offsetsToCommit);
            offsetsToCommit.clear();
        }
    }

    private void seekBackOnFailed() {
        failedRecords.forEach(failedRecord -> {
            TopicPartition topicPartition = new TopicPartition(failedRecord.topic(), failedRecord.partition());
            log.warn("Seeking back to offset " + failedRecord.offset() + " for: " + topicPartition);
            consumer.seek(topicPartition, failedRecord.offset());
        });
        failedRecords.clear();
    }

    private static void validateConfig(KafkaConsumerClientConfig<?, ?> config) {
        if (config.topics.isEmpty()) {
            throw new IllegalArgumentException("\"topics\" must contain at least 1 topic");
        }

        if (config.pollDurationMs <= 0) {
            throw new IllegalArgumentException("\"pollDurationMs\" must be larger than 0");
        }

        if (!Boolean.FALSE.equals(config.properties.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG))) {
            throw new IllegalArgumentException(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG + " must be false!");
        }
    }

}
