package no.nav.common.kafka.consumer.feilhandtering;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static no.nav.common.kafka.consumer.util.ConsumerUtils.mapFromStoredRecord;

public class KafkaConsumerRecordProcessor {

    private static final String FAILED_MESSAGES_METRIC = "kafka_consumer_failed_or_backedoff_messages_in_batch";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ExecutorService metricsExecutor = Executors.newSingleThreadExecutor();

    private final LockProvider lockProvider;

    private final KafkaConsumerRepository kafkaConsumerRepository;

    private final Map<String, TopicConsumer<byte[], byte[]>> topicConsumers;

    private final KafkaConsumerRecordProcessorConfig config;

    private volatile boolean isRunning;

    private final MeterRegistry meterRegistry;

    // Holder den autoritative DB-baserte verdien for hver (topic, partition).
    private final Map<TopicPartition, AtomicLong> failedMessagesGauges = new HashMap<>();

    public KafkaConsumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaRepository,
            Map<String, TopicConsumer<byte[], byte[]>> topicConsumers,
            KafkaConsumerRecordProcessorConfig config,
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        this.lockProvider = lockProvider;
        this.kafkaConsumerRepository = kafkaRepository;
        this.config = config;
        this.topicConsumers = topicConsumers;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start() {
        if (!isRunning) {
            executorService.submit(this::recordHandlerLoop);
            if (meterRegistry != null) {
                metricsExecutor.submit(this::metricsLoop);
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    private void recordHandlerLoop() {
        isRunning = true;

        List<String> topics = new ArrayList<>(topicConsumers.keySet());

        try {
            while (isRunning) {
                try {
                    List<TopicPartition> uniquePartitions = kafkaConsumerRepository.getTopicPartitions(topics);

                    consumeFromTopicPartitions(uniquePartitions);

                    Thread.sleep(config.pollTimeout.toMillis());
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

    /**
     * Egen løkke som med jevne mellomrom henter autoritative tall fra databasen og oppdaterer
     * gauges. Kjører uavhengig av Shedlock-låsen, slik at alle podder rapporterer samme verdi
     * og metrikken aldri blir "stuck" når låsen flytter seg eller når en partisjon forsvinner
     * fra arbeidssettet.
     */
    void metricsLoop() {
        List<String> topics = new ArrayList<>(topicConsumers.keySet());
        while (isRunning) {
            try {
                Map<TopicPartition, Long> counts = kafkaConsumerRepository.getFailedRecordCounts(topics);

                // Oppdater eller registrer gauges for partisjoner som har feilede meldinger nå
                counts.forEach(this::setFailedMessagesGauge);

                // Nullstill alle kjente partisjoner som ikke lenger har feilede meldinger
                failedMessagesGauges.forEach((tp, holder) -> {
                    if (!counts.containsKey(tp)) {
                        holder.set(0L);
                    }
                });

                Thread.sleep(config.pollTimeout.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Failed to update failed-or-backedoff metrics", e);
                try {
                    Thread.sleep(config.errorTimeout.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void setFailedMessagesGauge(TopicPartition topicPartition, Long value) {
        AtomicLong holder = failedMessagesGauges.computeIfAbsent(topicPartition, tp -> {
            AtomicLong newHolder = new AtomicLong();
            Gauge.builder(FAILED_MESSAGES_METRIC, newHolder, AtomicLong::get)
                    .tag("topic", tp.topic())
                    .tag("partition", String.valueOf(tp.partition()))
                    .strongReference(true)
                    .register(meterRegistry);
            return newHolder;
        });
        holder.set(value);
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
                        .getRecords(topicPartition.topic(), topicPartition.partition(), config.recordBatchSize);

                TopicConsumer<byte[], byte[]> recordConsumer = topicConsumers.get(topicPartition.topic());

                List<Long> recordsToDelete = new ArrayList<>();
                Set<Bytes> failedOrBackedOffKeys = new HashSet<>();

                records.forEach(record -> {
                    Bytes keyBytes = Bytes.wrap(record.getKey());

                    if (failedOrBackedOffKeys.contains(keyBytes)) {
                        return;
                    }

                    if (shouldBackoff(record)) {
                        if (keyBytes != null) {
                            failedOrBackedOffKeys.add(keyBytes);
                        }
                        return;
                    }

                    ConsumeStatus status;
                    Exception exception = null;

                    try {
                        status = recordConsumer.consume(mapFromStoredRecord(record));
                    } catch (Exception e) {
                        exception = e;
                        status = ConsumeStatus.FAILED;
                    }

                    if (status == ConsumeStatus.OK) {
                        log.info(
                                "Successfully processed stored record topic={} partition={} offset={} dbId={}",
                                record.getTopic(), record.getPartition(), record.getOffset(), record.getId()
                        );
                        recordsToDelete.add(record.getId());
                    } else {
                        String message = format(
                                "Failed to process stored consumer record topic=%s partition=%d offset=%d dbId=%d",
                                record.getTopic(), record.getPartition(), record.getOffset(), record.getId()
                        );

                        log.error(message, exception);

                        kafkaConsumerRepository.incrementRetries(record.getId());

                        if (keyBytes != null) {
                            failedOrBackedOffKeys.add(keyBytes);
                        }
                    }
                });

                if (!recordsToDelete.isEmpty()) {
                    kafkaConsumerRepository.deleteRecords(recordsToDelete);
                    log.info("Stored consumer records deleted {}", Arrays.toString(recordsToDelete.toArray()));
                }
            } catch (Exception e) {
                log.error("Unexpected exception caught while processing stored consumer records", e);
            } finally {
                lock.ifPresent(SimpleLock::unlock);
            }
        });
    }

    private boolean shouldBackoff(StoredConsumerRecord record) {
        Instant now = Instant.now();
        Instant nextAttempt = getLastAttempt(record).plus(config.backoffStrategy.getBackoffDuration(record));
        return now.isBefore(nextAttempt);
    }

    private Instant getLastAttempt(StoredConsumerRecord record) {
        if (record.getLastRetry() != null) {
            return record.getLastRetry().toInstant();
        } else {
            return Instant.ofEpochMilli(record.getTimestamp());
        }
    }

    private Optional<SimpleLock> acquireLock(TopicPartition topicPartition) {
        String name = "kcrp-" + topicPartition.topic() + "-" + topicPartition.partition();
        LockConfiguration configuration = new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ofSeconds(5));
        return lockProvider.lock(configuration);
    }

}
