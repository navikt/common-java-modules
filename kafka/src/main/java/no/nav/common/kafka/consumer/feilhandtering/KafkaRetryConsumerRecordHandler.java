package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KafkaRetryConsumerRecordHandler<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<String, TopicConsumer<K, V>> topics;

    private final KafkaConsumerRepository<K, V> kafkaConsumerRepository;

    public KafkaRetryConsumerRecordHandler(Map<String, TopicConsumer<K, V>> topics, KafkaConsumerRepository<K, V> kafkaConsumerRepository) {
        this.topics = topics;
        this.kafkaConsumerRepository = kafkaConsumerRepository;
    }

    public void consumeFailedMessages() {
        List<String> topicNames = new ArrayList<>(topics.keySet());
        List<KafkaConsumerRecord<K, V>> records = kafkaConsumerRepository.getRecords(topicNames, 100);

        records.forEach(record -> {
            TopicConsumer<K, V> consumer = topics.get(record.topic);

            if (consumer == null) {
                log.warn("Could not find consumer for topic " + record.topic);
                return;
            }

            ConsumeStatus status = ConsumerUtils.safeConsume(consumer, ConsumerUtils.mapRecord(record));

            if (status == ConsumeStatus.FAILED) {
                log.warn("Failed to consume previously failed message. topic={} partition={} offset={}", record.topic, record.partition, record.offset);
                return;
            }

            kafkaConsumerRepository.deleteRecord(record.id);
        });
    }

}
