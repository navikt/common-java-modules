package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.ProducerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;

class BatchedKafkaProducerRecordPublisher implements KafkaProducerRecordPublisher {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final KafkaProducerClient<byte[], byte[]> producerClient;

    public BatchedKafkaProducerRecordPublisher(KafkaProducerClient<byte[], byte[]> producerClient) {
        this.producerClient = producerClient;
    }

    @Override
    public List<Long> publishStoredRecords(List<StoredProducerRecord> records) {
        ConcurrentLinkedQueue<Long> idsToDelete = new ConcurrentLinkedQueue<>();

        CountDownLatch latch = new CountDownLatch(records.size());

        records.forEach(record -> {
            producerClient.send(ProducerUtils.mapFromStoredRecord(record), (metadata, exception) -> {
                latch.countDown();

                if (exception != null) {
                    log.warn("Failed to resend failed record to topic {}", record.getTopic(), exception);
                } else {
                    idsToDelete.add(record.getId());
                }
            });
        });

        producerClient.getProducer().flush();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while waiting for Kafka messages to be sent", e);
        }

        return new ArrayList<>(idsToDelete);
    }

    @Override
    public void close() {
        producerClient.close();
    }
}
