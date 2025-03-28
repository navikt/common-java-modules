package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.publisher.BatchedKafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.publisher.KafkaProducerRecordPublisher;
import no.nav.common.kafka.producer.feilhandtering.util.KafkaProducerRecordProcessorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaProducerRecordProcessor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final long errorTimeoutMs;
    private final long pollTimeoutMs;
    private final long waitingForLeaderTimeoutMs;
    private final int recordsBatchSize;
    private final KafkaProducerRepository producerRepository;
    private final KafkaProducerRecordPublisher kafkaProducerRecordPublisher;
    private final LeaderElectionClient leaderElectionClient;
    // If the list is not null then it will be used to filter which records will be sent to Kafka
    private final List<String> topicWhitelist;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isRunning;
    private volatile boolean isClosed;

    public KafkaProducerRecordProcessor(
            long errorTimeoutMs,
            long pollTimeoutMs,
            long waitingForLeaderTimeoutMs,
            int recordsBatchSize,
            boolean registerShutdownHook,
            KafkaProducerRepository producerRepository,
            KafkaProducerRecordPublisher kafkaProducerRecordPublisher,
            LeaderElectionClient leaderElectionClient,
            List<String> topicWhitelist
    ) {
        this.errorTimeoutMs = errorTimeoutMs;
        this.pollTimeoutMs = pollTimeoutMs;
        this.waitingForLeaderTimeoutMs = waitingForLeaderTimeoutMs;
        this.recordsBatchSize = recordsBatchSize;
        this.producerRepository = producerRepository;
        this.kafkaProducerRecordPublisher = kafkaProducerRecordPublisher;
        this.leaderElectionClient = leaderElectionClient;
        this.topicWhitelist = topicWhitelist;

        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        }
    }

    public KafkaProducerRecordProcessor(
            KafkaProducerRepository producerRepository,
            KafkaProducerClient<byte[], byte[]> producerClient,
            LeaderElectionClient leaderElectionClient,
            List<String> topicWhitelist
    ) {
        this(
                KafkaProducerRecordProcessorBuilder.DEFAULT_ERROR_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_POLL_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_WAITING_FOR_LEADER_TIMEOUT_MS,
                KafkaProducerRecordProcessorBuilder.DEFAULT_RECORDS_BATCH_SIZE,
                KafkaProducerRecordProcessorBuilder.DEFAULT_REGISTER_SHUTDOWN_HOOK,
                producerRepository,
                new BatchedKafkaProducerRecordPublisher(producerClient),
                leaderElectionClient,
                topicWhitelist
        );
    }

    public KafkaProducerRecordProcessor(
            KafkaProducerRepository producerRepository,
            KafkaProducerClient<byte[], byte[]> producerClient,
            LeaderElectionClient leaderElectionClient
    ) {
        this(producerRepository, producerClient, leaderElectionClient, null);
    }

    public void start() {
        if (isClosed) {
            throw new IllegalStateException("Cannot start closed producer record processor");
        }

        if (!isRunning) {
            executorService.submit(this::recordHandlerLoop);
        }
    }

    public void close() {
        log.info("Closing kafka producer record processor...");
        isRunning = false;
        isClosed = true;
    }

    private void recordHandlerLoop() {
        isRunning = true;

        try {
            while (isRunning) {
                try {
                    if (!leaderElectionClient.isLeader()) {
                        Thread.sleep(waitingForLeaderTimeoutMs);
                        continue;
                    }

                    List<StoredProducerRecord> records = topicWhitelist == null
                            ? producerRepository.getRecords(recordsBatchSize)
                            : producerRepository.getRecords(recordsBatchSize, topicWhitelist);

                    if (!records.isEmpty()) {
                        publishStoredRecords(records);
                    }

                    // If the number of records are less than the max batch size,
                    //   then most likely there are not many messages to process and we can wait a bit
                    if (records.size() < recordsBatchSize) {
                        Thread.sleep(pollTimeoutMs);
                    }
                } catch (Exception e) {
                    log.error("Failed to process kafka producer records", e);
                    Thread.sleep(errorTimeoutMs);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught in producer record handler loop", e);
        } finally {
            try {
                kafkaProducerRecordPublisher.close();
            } catch (IOException e) {
                log.error("Failed to close kafka producer record publisher", e);
            }
        }
    }

    private void publishStoredRecords(List<StoredProducerRecord> records) {
        var idsToDelete = kafkaProducerRecordPublisher.publishStoredRecords(records);
        producerRepository.deleteRecords(idsToDelete);
    }
}
