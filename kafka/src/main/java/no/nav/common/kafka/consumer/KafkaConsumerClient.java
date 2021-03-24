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
 * @param <K> topic key
 * @param <V> topic value
 */
public class KafkaConsumerClient<K, V> implements ConsumerRebalanceListener {

    public final static long DEFAULT_POLL_DURATION_MS = 1000;

    private final static long POLL_ERROR_TIMEOUT_MS = 5000;

    private enum ClientState {
       RUNNING, NOT_RUNNING
    }

    private final Logger log = LoggerFactory.getLogger(KafkaConsumerClient.class);

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    private final Set<TopicPartition> revokedOrFailedPartitions = ConcurrentHashMap.newKeySet();

    private final KafkaConsumerClientConfig<K, V> config;

    private volatile ClientState clientState = ClientState.NOT_RUNNING;

    private volatile CountDownLatch processedRecordsLatch;

    private volatile CountDownLatch shutdownLatch;

    private KafkaConsumer<K, V> consumer;

    public KafkaConsumerClient(KafkaConsumerClientConfig<K, V> config) {
        validateConfig(config);

        this.config = config;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        if (clientState == ClientState.RUNNING) {
            return;
        }

        clientState = ClientState.RUNNING;

        log.info("Starting kafka consumer client...");

        pollExecutor.submit(this::consumeTopics);
    }

    public void stop() {
        if (clientState != ClientState.RUNNING) {
            return;
        }

        clientState = ClientState.NOT_RUNNING;

        log.info("Stopping kafka consumer client...");

        try {
            consumer.wakeup(); // Will abort an ongoing consumer.poll()
            shutdownLatch.await(10, TimeUnit.SECONDS);
            log.info("Kafka client stopped");
        } catch (InterruptedException e) {
            log.error("Failed to stop gracefully", e);
        }
    }

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
            log.error("Failed to commit offsets when partitions were revoked: " + currentOffsets.toString(), e);
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

                revokedOrFailedPartitions.clear();
                currentOffsets.clear();

                try {
                    records = consumer.poll(Duration.ofMillis(config.pollDurationMs));
                } catch (WakeupException e) {
                  log.info("Polling was cancelled by wakeup(). Stopping kafka consumer...");
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
                            if (clientState == ClientState.NOT_RUNNING || revokedOrFailedPartitions.contains(topicPartition)) {
                                return;
                            }

                            ConsumeStatus status = ConsumerUtils.safeConsume(topicConsumer, record);

                            if (status == ConsumeStatus.OK) {
                                OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(record.offset());
                                currentOffsets.put(topicPartition, offsetAndMetadata);
                            } else {
                                revokedOrFailedPartitions.add(topicPartition);
                            }
                        } catch (Exception e) {
                            String msg = format(
                                    "Unexpected error occurred wile processing record. topic=%s partition=%d offset=%d",
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
                } catch (Exception e) {
                    // If we fail to commit offsets then continue polling records
                    log.error("Failed to commit offsets: " + currentOffsets.toString(), e);
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
                log.error("Failed to shutdown properly", e);
            } finally {
                shutdownLatch.countDown();

                if (clientState == ClientState.RUNNING) {
                    log.warn("Unexpected failure while client was running. Restarting...");
                    pollExecutor.submit(this::consumeTopics);
                }
            }
        }
    }

    private void commitCurrentOffsets() {
        if (!currentOffsets.isEmpty()) {
            consumer.commitSync(currentOffsets, Duration.ofSeconds(3));
            log.info("Offsets committed: " + currentOffsets.toString());
            currentOffsets.clear();
        }
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
