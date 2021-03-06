package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.mapToStoredRecord;

/**
 * Wraps a TopicConsumer and stores the record into a database if the underlying consumer fails to process the record.
 * To ensure that records are processed in the proper order, if there is a record with the same topic+partition+key already stored,
 * then the underlying consumer will be skipped and the record will be stored.
 */
public class StoreOnFailureTopicConsumer implements TopicConsumer<byte[], byte[]> {

    private final TopicConsumer<byte[], byte[]> consumer;

    private final KafkaConsumerRepository consumerRepository;


    public StoreOnFailureTopicConsumer(
            TopicConsumer<byte[], byte[]> consumer,
            KafkaConsumerRepository consumerRepository
    ) {
        this.consumer = consumer;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<byte[], byte[]> record) {
        boolean shouldConsumeRecord =
                record.key() == null ||
                !consumerRepository.hasRecordWithKey(record.topic(), record.partition(), record.key());

        /*
            If we consumed the record while there already was a record with the same key in the database,
            then the consumption would be out-of-order.
            If the key is null, then the message can be consumed even if there is a record in the database.
         */
        if (shouldConsumeRecord) {
            ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);

            if (status == ConsumeStatus.OK) {
                return ConsumeStatus.OK;
            }
        }

        consumerRepository.storeRecord(mapToStoredRecord(record));
        return ConsumeStatus.OK;
    }

}
