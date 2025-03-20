package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.ProducerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class KafkaProducerRecordProcessor {

    private final static long ERROR_TIMEOUT_MS = 5000;

    private final static long POLL_TIMEOUT_MS = 3000;

    private final static long WAITING_FOR_LEADER_TIMEOUT_MS = 10_000;

    private final static int RECORDS_BATCH_SIZE = 100;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final KafkaProducerRepository producerRepository;

    private final KafkaProducerClient<byte[], byte[]> producerClient;

    private final LeaderElectionClient leaderElectionClient;

    // If the list is not null then it will be used to filter which records will be sent to Kafka
    private final List<String> topicWhitelist;

    private volatile boolean isRunning;

    private volatile boolean isClosed;

    public KafkaProducerRecordProcessor(
            KafkaProducerRepository producerRepository,
            KafkaProducerClient<byte[], byte[]> producerClient,
            LeaderElectionClient leaderElectionClient,
            List<String> topicWhitelist
    ) {
        this.producerRepository = producerRepository;
        this.producerClient = producerClient;
        this.leaderElectionClient = leaderElectionClient;
        this.topicWhitelist = topicWhitelist;

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
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
                        Thread.sleep(WAITING_FOR_LEADER_TIMEOUT_MS);
                        continue;
                    }

                    List<StoredProducerRecord> records = topicWhitelist == null
                            ? producerRepository.getRecords(RECORDS_BATCH_SIZE)
                            : producerRepository.getRecords(RECORDS_BATCH_SIZE, topicWhitelist);

                    if (!records.isEmpty()) {
                        publishStoredRecordsBatch(records);
                    }

                    // If the number of records are less than the max batch size,
                    //   then most likely there are not many messages to process and we can wait a bit
                    if (records.size() < RECORDS_BATCH_SIZE) {
                        Thread.sleep(POLL_TIMEOUT_MS);
                    }
                } catch (Exception e) {
                    log.error("Failed to process kafka producer records", e);
                    Thread.sleep(ERROR_TIMEOUT_MS);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected exception caught in producer record handler loop", e);
        } finally {
            producerClient.close();
        }
    }

    private void publishStoredRecordsBatch(List<StoredProducerRecord> records) throws InterruptedException {
        /* TODO
            Sending batches could also be done in a transaction.
            This would make the batches idempotent, and only produce 1 message once.
            It would also ensure that all messages are sent atomically and that all messages are either sent or not sent.
        */

        ConcurrentLinkedQueue<Long> idsToDelete = new ConcurrentLinkedQueue<>();

        CountDownLatch latch = new CountDownLatch(records.size());

        records.forEach(record -> {
            producerClient.send(ProducerUtils.mapFromStoredRecord(record), (metadata, exception) -> {
                latch.countDown();

                if (exception != null) {
                    log.warn(format("Failed to resend failed record to topic %s", record.getTopic()), exception);
                } else {
                    idsToDelete.add(record.getId());
                }
            });
        });

        producerClient.getProducer().flush();

        latch.await();

        producerRepository.deleteRecords(new ArrayList<>(idsToDelete));

    }

}
