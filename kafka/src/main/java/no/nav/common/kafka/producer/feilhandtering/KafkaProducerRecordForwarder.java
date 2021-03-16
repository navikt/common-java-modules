package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.util.ProducerUtils;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class KafkaProducerRecordForwarder {

    private final static long ERROR_TIMEOUT_MS = 5000;

    private final static long POLL_TIMEOUT_MS = 3000;

    private final static long RECORDS_OLDER_THAN_MS = 0;

    private final static int RECORDS_BATCH_SIZE = 100;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final KafkaProducerRepository<byte[], byte[]> kafkaRepository;

    private final Producer<byte[], byte[]> producer;

    private volatile boolean isRunning;

    private volatile boolean isClosed;

    public KafkaProducerRecordForwarder(
            KafkaProducerRepository<byte[], byte[]> kafkaRepository,
            Producer<byte[], byte[]> producerClient
    ) {
        this.kafkaRepository = kafkaRepository;
        this.producer = producerClient;

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void start() {
        if (isClosed) {
            throw new IllegalStateException("Cannot start closed producer record forwarder");
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

        try {
           while (isRunning) {
               try {
                   Instant recordsOlderThan = Instant.now().minusMillis(RECORDS_OLDER_THAN_MS);
                   List<KafkaProducerRecord<byte[], byte[]>> records = kafkaRepository.getRecords(recordsOlderThan, RECORDS_BATCH_SIZE);

                   if (!records.isEmpty()) {
                       publishStoredRecordsBatch(records);
                   }

                   // If the number of records are less than the max batch size,
                   //   then most likely there are not many messages to process and we can wait a bit
                   if (records.size() < RECORDS_BATCH_SIZE) {
                       Thread.sleep(POLL_TIMEOUT_MS);
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
           producer.close();
       }
    }

    private void publishStoredRecordsBatch(List<KafkaProducerRecord<byte[], byte[]>> records) throws InterruptedException {
        // TODO: could be done inside a kafka transaction

        CountDownLatch latch = new CountDownLatch(records.size());

        records.forEach(record -> {
            producer.send(ProducerUtils.mapRecord(record), (metadata, exception) -> {
                try {
                    if (exception != null) {
                        log.warn(format("Failed to resend failed message to topic %s", record.getTopic()), exception);
                    } else {
                        kafkaRepository.deleteRecord(record.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to send message to kafka", e);
                } finally {
                    latch.countDown();
                }
            });
        });

        producer.flush();

        latch.await();
    }

}
