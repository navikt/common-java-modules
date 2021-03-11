package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreOnFailureTopicConsumer<K, V> implements TopicConsumer<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final KafkaConsumerRepository<K, V> consumerRepository;

    private final TopicConsumer<K, V> consumer;

    public StoreOnFailureTopicConsumer(KafkaConsumerRepository<K, V> consumerRepository, TopicConsumer<K, V> consumer) {
        this.consumerRepository = consumerRepository;
        this.consumer = consumer;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<K, V> record) {
        ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);

        if (status == ConsumeStatus.OK) {
            return ConsumeStatus.OK;
        }

        try {
            // TODO: Try to add constraint on topic/partition/offset
            consumerRepository.storeRecord(record);
            return ConsumeStatus.OK;
        } catch (Exception e) {
            log.error("Unable to store failed message in database", e);
            return ConsumeStatus.FAILED;
        }
    }
}
