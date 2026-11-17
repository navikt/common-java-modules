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
import java.util.function.Supplier;

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

    public static final long DEFAULT_POLL_DURATION_MS = 1000;

    private static final long POLL_ERROR_TIMEOUT_MS = 5000;

    private enum ClientState {
        RUNNING, NOT_RUNNING
    }

    private final Logger log = LoggerFactory.getLogger(KafkaConsumerClientImpl.class);

    private ExecutorService pollExecutor;

    private final long shutdownTimeoutMillis;

    private final Object consumerLock = new Object();

    private final Supplier<KafkaConsumer<K, V>> consumerFactory;

    private volatile boolean consumerCleanupRequired;

    private final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();

    private final KafkaConsumerClientConfig<K, V> config;

    private volatile ClientState clientState = ClientState.NOT_RUNNING;

    private volatile CountDownLatch processedRecordsLatch;

    private KafkaConsumer<K, V> consumer;

    public KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config) {
        this(config, TimeUnit.SECONDS.toMillis(10));
    }

    KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, long shutdownTimeoutMillis) {
        this(config, shutdownTimeoutMillis, () -> new KafkaConsumer<>(config.properties));
    }

    KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, long shutdownTimeoutMillis,
                           Supplier<KafkaConsumer<K, V>> consumerFactory) {
        validateConfig(config);

        this.config = config;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
        this.consumerFactory = consumerFactory;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public synchronized void start() {
        if (clientState == ClientState.RUNNING) {
            return;
        }
        if (pollExecutor != null && !pollExecutor.isTerminated()) {
            throw new IllegalStateException("Previous kafka consumer run has not terminated; call stop() again");
        }
        if (consumerCleanupRequired) {
            throw new IllegalStateException("Previous kafka consumer has not closed; call stop() again");
        }
        try {
            openConsumer(false);
            pollExecutor = Executors.newSingleThreadExecutor();
            clientState = ClientState.RUNNING;
            log.info("Starting kafka consumer client...");
            pollExecutor.submit(this::consumeTopics);
        } catch (RuntimeException | Error failure) {
            clientState = ClientState.NOT_RUNNING;
            if (pollExecutor != null) {
                pollExecutor.shutdown();
            }
            closeConsumerAfterFailure(failure);
            throw failure;
        }
    }

    @Override
    public synchronized void stop() {
        if ((pollExecutor == null || pollExecutor.isTerminated()) && !consumerCleanupRequired) {
            return;
        }

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(shutdownTimeoutMillis);
        boolean wasRunning = clientState == ClientState.RUNNING;
        clientState = ClientState.NOT_RUNNING;
        if (pollExecutor != null) {
            pollExecutor.shutdown();
        }

        log.info("Stopping kafka consumer client...");

        try {
            if (wasRunning) {
                synchronized (consumerLock) {
                    if (consumerCleanupRequired) {
                        consumer.wakeup(); // Will abort an ongoing consumer.poll()
                    }
                }
            }
            if (pollExecutor != null && !pollExecutor.awaitTermination(
                    Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                throw new IllegalStateException("Timed out stopping kafka consumer client; processing may still be in progress");
            }
            if (consumerCleanupRequired) {
                closeConsumer(Duration.ofNanos(Math.min(TimeUnit.SECONDS.toNanos(3),
                        Math.max(0, deadline - System.nanoTime()))));
            }
            log.info("Kafka consumer client stopped");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping kafka consumer client", e);
        }
    }

    @Override
    public boolean isRunning() {
        return clientState == ClientState.RUNNING;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions has been revoked from consumer: " + Arrays.toString(partitions.toArray()));
        revokedOrFailedPartitions.addAll(partitions);
        try {
            commitCurrentOffsets();
        } catch (Exception e) {
            log.error("Failed to commit offsets when partitions were revoked: " + offsetsToCommit, e);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("New partitions has been assigned to the consumer: " + Arrays.toString(partitions.toArray()));
    }

    private void consumeTopics() {
        try {
            while (clientState == ClientState.RUNNING) {
                consumeSession();
                if (clientState == ClientState.RUNNING) {
                    log.warn("Unexpected failure while kafka consumer client was running. Restarting...");
                }
                boolean opened = false;
                while (clientState == ClientState.RUNNING && !opened) {
                    try {
                        if (consumerCleanupRequired) {
                            closeConsumer(Duration.ofSeconds(3));
                        }
                        if (clientState == ClientState.RUNNING) {
                            openConsumer(true);
                            opened = true;
                        }
                    } catch (RuntimeException | Error failure) {
                        closeConsumerAfterFailure(failure);
                        log.error("Failed to restart kafka consumer client", failure);
                        if (failure instanceof Error) {
                            throw (Error) failure;
                        }
                    }
                }
            }
        } finally {
            try {
                if (consumerCleanupRequired) {
                    closeConsumer(Duration.ofSeconds(3));
                }
            } catch (Exception e) {
                log.error("Failed to close kafka consumer client", e);
            } finally {
                clientState = ClientState.NOT_RUNNING;
                pollExecutor.shutdown();
            }
        }
    }

    private void consumeSession() {

        final Map<TopicPartition, ExecutorService> topicConsumptionExecutors = new HashMap<>();

        try {

            while (clientState == ClientState.RUNNING) {
                ConsumerRecords<K, V> records;

                revokedOrFailedPartitions.clear();
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

                /*
                 We seek to the offset of the first record in each topic+partition so that all the records in the batch must
                 be processed before polling new records.

                 If this is not done then it is possible for records from revoked partitions or failed records to be skipped if
                 no other record for that topic+partition has been added to offsetsToCommit.
                */
                seekToFirstRecordOffsetForEachTopicPartition(records);

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
                            if (clientState == ClientState.NOT_RUNNING || revokedOrFailedPartitions.contains(topicPartition)) {
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
                                revokedOrFailedPartitions.add(topicPartition);
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
                log.debug("ProcessedRecordsLatch.await start");
                var awaitStart = System.currentTimeMillis();
                processedRecordsLatch.await();
                log.debug("ProcessedRecordsLatch.await: finished after {} ms", System.currentTimeMillis() - awaitStart);

                try {
                    commitCurrentOffsets();
                } catch (Exception e) {
                    // If we fail to commit offsets then continue polling records
                    log.error("Failed to commit offsets: " + offsetsToCommit, e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught from main loop. Shutting down...", e);
        } finally {
            try {
                topicConsumptionExecutors.forEach(((topicPartition, executorService) -> executorService.shutdown()));
                // Never close/commit while an in-flight handler may still update its offset.
                boolean interrupted = false;
                for (ExecutorService executor : topicConsumptionExecutors.values()) {
                    while (!executor.isTerminated()) {
                        try {
                            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                try {
                    commitCurrentOffsets();
                } catch (RuntimeException | Error failure) {
                    closeConsumerAfterFailure(failure);
                    throw failure;
                }
                closeConsumer(Duration.ofSeconds(3));
            } catch (Exception e) {
                log.error("Failed to shutdown kafka consumer client properly", e);
            }
        }
    }

    private void openConsumer(boolean restarting) {
        KafkaConsumer<K, V> nextConsumer = consumerFactory.get();
        synchronized (consumerLock) {
            consumer = nextConsumer;
            consumerCleanupRequired = true;
        }
        if (!restarting || clientState == ClientState.RUNNING) {
            nextConsumer.subscribe(new ArrayList<>(config.topics.keySet()), this);
        }
    }

    private void closeConsumer(Duration timeout) {
        consumer.close(timeout);
        consumerCleanupRequired = false;
    }

    private void closeConsumerAfterFailure(Throwable failure) {
        if (!consumerCleanupRequired) {
            return;
        }
        try {
            closeConsumer(Duration.ofSeconds(3));
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private void commitCurrentOffsets() {
        if (!offsetsToCommit.isEmpty()) {
            safeSeek(offsetsToCommit);

            consumer.commitSync(offsetsToCommit, Duration.ofSeconds(3));
            log.info("Offsets committed: " + offsetsToCommit);
            offsetsToCommit.clear();
        }
    }

    private void seekToFirstRecordOffsetForEachTopicPartition(ConsumerRecords<K, V> records) {
        Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>();

        records.forEach(record -> {
            TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
            OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(record.offset());

            OffsetAndMetadata storedOffset = offsetMap.get(topicPartition);

            if (storedOffset == null || offsetAndMetadata.offset() < storedOffset.offset()) {
                offsetMap.put(topicPartition, offsetAndMetadata);
            }
        });

        safeSeek(offsetMap);
    }

    private void safeSeek(Map<TopicPartition, OffsetAndMetadata> offsetMap) {
        offsetMap.forEach((topicPartition, offsetAndMetadata) -> {
            try {
                consumer.seek(topicPartition, offsetAndMetadata);
            } catch (IllegalStateException e) {
                log.warn(
                        "Unable to seek to topicPartition={} offset={}. The TopicPartition is not assigned to this consumer",
                        topicPartition, offsetAndMetadata.offset()
                );
            }
        });
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
