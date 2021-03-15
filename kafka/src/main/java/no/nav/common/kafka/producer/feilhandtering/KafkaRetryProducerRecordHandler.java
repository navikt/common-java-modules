package no.nav.common.kafka.producer.feilhandtering;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.ProducerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

import static java.lang.String.format;

public class KafkaRetryProducerRecordHandler<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final List<String> topics;

    private final KafkaProducerRepository<K, V> kafkaRepository;

    private final KafkaProducerClient<K, V> producerClient;

    public KafkaRetryProducerRecordHandler(
            List<String> topics,
            KafkaProducerRepository<K, V> kafkaRepository,
            KafkaProducerClient<K, V> producerClient
    ) {
        this.topics = topics;
        this.kafkaRepository = kafkaRepository;
        this.producerClient = producerClient;
    }

    public void sendFailedMessages() {
        List<KafkaProducerRecord<K, V>> records = kafkaRepository.getRecords(topics, Instant.now().minusSeconds(30), 100);

        records.forEach(record -> {
            producerClient.send(ProducerUtils.mapRecord(record), (metadata, exception) -> {
                if (exception != null) {
                    log.warn(format("Failed to resend failed message to topic %s", record.getTopic()), exception);
                } else {
                    kafkaRepository.deleteRecord(record.getId());
                }
            });
        });
    }

}
