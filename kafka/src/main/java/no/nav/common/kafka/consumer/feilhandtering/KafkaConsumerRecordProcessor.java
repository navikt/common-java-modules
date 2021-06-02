package no.nav.common.kafka.consumer.feilhandtering;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static no.nav.common.kafka.consumer.util.ConsumerUtils.mapFromStoredRecord;

public class KafkaConsumerRecordProcessor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final LockProvider lockProvider;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    private final Map<String, TopicConsumer<byte[], byte[]>> topicConsumers;

    private final KafkaConsumerRecordProcessorConfig config;

    private volatile boolean isRunning;

    private volatile boolean isClosed;

    public KafkaConsumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaRepository,
            Map<String, TopicConsumer<byte[], byte[]>> topicConsumers,
            KafkaConsumerRecordProcessorConfig config
    ) {
        this.lockProvider = lockProvider;
        this.kafkaConsumerRepository = kafkaRepository;
        this.config = config;
        this.topicConsumers = topicConsumers;

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void start() {
        if (isClosed) {
            throw new IllegalStateException("Cannot start closed consumer record forwarder");
        }

        if (!isRunning) {
            executorService.submit(this::recordHandlerLoop);
        }
    }

    public void close() {
        isRunning = false;
        isClosed = true;
    }

    private void recordHandlerLoop() {
        isRunning = true;

        List<String> topics = new ArrayList<>(topicConsumers.keySet());

        try {
            while (isRunning) {
                try {
                    List<TopicPartition> uniquePartitions = kafkaConsumerRepository.getTopicPartitions(topics);

                    if (uniquePartitions.isEmpty()) {
                        Thread.sleep(config.pollTimeout.toMillis());
                    } else {
                        boolean haveAllSucceeded = consumeFromTopicPartitions(uniquePartitions);
                        if (!haveAllSucceeded) {
                            Thread.sleep(config.errorTimeout.toMillis());
                        }
                    }

                } catch (Exception e) {
                    log.error("Failed to consume stored kafka records", e);
                    Thread.sleep(config.errorTimeout.toMillis());
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught in stored consumer record handler loop", e);
        } finally {
            log.info("Closing kafka consumer record processor...");
        }
    }

    private boolean consumeFromTopicPartitions(List<TopicPartition> uniquePartitions) {
        AtomicBoolean haveAllSucceeded = new AtomicBoolean(true);
        uniquePartitions.forEach(topicPartition -> {
            if (!isRunning) {
                return;
            }

            Optional<SimpleLock> lock = Optional.empty();
            try {
                lock = acquireLock(topicPartition);

                // Lock has already been acquired by someone else
                if (lock.isEmpty()) {
                    return;
                }

                List<StoredConsumerRecord> records = kafkaConsumerRepository
                        .getRecords(topicPartition.topic(), topicPartition.partition(), config.recordBatchSize);

                TopicConsumer<byte[], byte[]> recordConsumer = topicConsumers.get(topicPartition.topic());

                List<Long> recordsToDelete = new ArrayList<>();
                Map<TopicPartition, Set<Bytes>> failedKeys = new HashMap<>();

                records.forEach(r -> {
                    Set<Bytes> keySet = failedKeys.get(topicPartition);

                    Bytes keyBytes = Bytes.wrap(r.getKey());

                    // We cannot process records where a previous record with the same key (and topic+partition) has failed to be consumed
                    if (keySet != null && keyBytes != null && keySet.contains(keyBytes)) {
                        return;
                    }

                    // TODO: Can implement exponential backoff if necessary

                    ConsumeStatus status;
                    Exception exception = null;

                    try {
                        status = recordConsumer.consume(mapFromStoredRecord(r));
                    } catch (Exception e) {
                        exception = e;
                        status = ConsumeStatus.FAILED;
                    }

                    if (status == ConsumeStatus.OK) {
                        log.info(
                                "Successfully process stored record topic={} partition={} offset={} dbId={}",
                                r.getTopic(), r.getPartition(), r.getOffset(), r.getId()
                        );
                        recordsToDelete.add(r.getId());
                    } else {
                        String message = format(
                                "Failed to process stored consumer record topic=%s partition=%d offset=%d dbId=%d",
                                r.getTopic(), r.getPartition(), r.getOffset(), r.getId()
                        );

                        haveAllSucceeded.set(false);
                        log.error(message, exception);
                        kafkaConsumerRepository.incrementRetries(r.getId());
                        if (keyBytes != null) {
                            failedKeys.computeIfAbsent(topicPartition, (_ignored) -> new HashSet<>()).add(keyBytes);
                        }
                    }
                });

                if (!recordsToDelete.isEmpty()) {
                    kafkaConsumerRepository.deleteRecords(recordsToDelete);
                    log.info("Stored consumer records deleted " + Arrays.toString(recordsToDelete.toArray()));
                }

            } catch (Exception e) {
                log.error("Unexpected exception caught while processing stored consumer records", e);
            } finally {
                lock.ifPresent(SimpleLock::unlock);
            }
        });
        return haveAllSucceeded.get();
    }

    private Optional<SimpleLock> acquireLock(TopicPartition topicPartition) {
        String name = "kcrp-" + topicPartition.topic() + "-" + topicPartition.partition();
        LockConfiguration configuration = new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ofSeconds(5));
        return lockProvider.lock(configuration);
    }

}
