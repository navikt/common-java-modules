package no.nav.common.kafka.consumer.feilhandtering;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaConsumerRecordProcessor {

    private final static long ERROR_TIMEOUT_MS = 5000;

    private final static long POLL_TIMEOUT_MS = 3000;

    private final static int RECORDS_BATCH_SIZE = 100;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final LockProvider lockProvider;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    private final Map<String, StoredRecordConsumer> recordConsumers;

    private volatile boolean isRunning;

    private volatile boolean isClosed;

    public KafkaConsumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaRepository,
            Map<String, StoredRecordConsumer> recordConsumers
    ) {
        this.lockProvider = lockProvider;
        this.kafkaConsumerRepository = kafkaRepository;
        this.recordConsumers = recordConsumers;
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

        List<String> topics = new ArrayList<>(recordConsumers.keySet());

        try {
            while (isRunning) {
                try {
                    List<TopicPartition> uniquePartitions = kafkaConsumerRepository.getTopicPartitions(topics);

                    if (uniquePartitions.isEmpty()) {
                        Thread.sleep(POLL_TIMEOUT_MS);
                    } else {
                        consumeFromTopicPartitions(uniquePartitions);
                    }

                } catch (Exception e) {
                    log.error("Failed to forward kafka records", e);
                    Thread.sleep(ERROR_TIMEOUT_MS);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught in record handler loop", e);
        } finally {
            log.info("Closing kafka producer record forwarder...");
        }
    }

    private void consumeFromTopicPartitions(List<TopicPartition> uniquePartitions) {
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
                        .getRecords(topicPartition.topic(), topicPartition.partition(), RECORDS_BATCH_SIZE);

                StoredRecordConsumer recordConsumer = recordConsumers.get(topicPartition.topic());

                List<Long> recordsToDelete = new ArrayList<>();
                Map<TopicPartition, Set<Bytes>> failedKeys = new HashMap<>();

                records.forEach(r -> {
                    Set<Bytes> keySet = failedKeys.get(topicPartition);

                    // We cannot process records where a previous record with the same key (and topic+partition) has failed to be consumed
                    if (keySet != null && keySet.contains(Bytes.wrap(r.getKey()))) {
                        return;
                    }

                    // TODO: Can implement exponential backoff if necessary

                    ConsumeStatus status;

                    try {
                        status = recordConsumer.consume(r);
                    } catch (Exception e) {
                        status = ConsumeStatus.FAILED;
                    }

                    if (status == ConsumeStatus.OK) {
                        recordsToDelete.add(r.getId());
                    } else {
                        log.error(
                                "Failed to process consumer record topic={} partition={} offset={} dbId={}",
                                r.getTopic(), r.getPartition(), r.getOffset(), r.getId()
                        );
                        kafkaConsumerRepository.incrementRetries(r.getId());
                        failedKeys.computeIfAbsent(topicPartition, (_ignored) -> new HashSet<>()).add(Bytes.wrap(r.getKey()));
                    }
                });

                if (!recordsToDelete.isEmpty()) {
                    kafkaConsumerRepository.deleteRecords(recordsToDelete);
                    log.info("Consumed records deleted " + Arrays.toString(recordsToDelete.toArray()));
                }

            } catch (Exception e) {
                log.error("Unexpected exception caught while processing consumer records", e);
            } finally {
                lock.ifPresent(SimpleLock::unlock);
            }
        });
    }

    private Optional<SimpleLock> acquireLock(TopicPartition topicPartition) {
        String name = "kcrp-" + topicPartition.topic() + "-" + topicPartition.partition();
        LockConfiguration configuration = new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ofSeconds(5));
        return lockProvider.lock(configuration);
    }

}
