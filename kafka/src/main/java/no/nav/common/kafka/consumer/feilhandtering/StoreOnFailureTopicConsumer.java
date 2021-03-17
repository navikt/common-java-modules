package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serializer;

public class StoreOnFailureTopicConsumer<K, V> implements TopicConsumer<K, V> {

    private final TopicConsumer<K, V> consumer;

    private final KafkaConsumerRepository consumerRepository;

    private final Serializer<K> keySerializer;

    private final Serializer<V> valueSerializer;

    public StoreOnFailureTopicConsumer(
            TopicConsumer<K, V> consumer,
            KafkaConsumerRepository consumerRepository,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer
    ) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.consumer = consumer;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<K, V> record) {
        byte[] key = keySerializer.serialize(record.topic(), record.key());
        boolean hasRecord = consumerRepository.hasRecordWithKey(record.topic(), record.partition(), key);

        /*
            If we consumed the record while there already was a record with the same key in the database,
            then the consumption would be out-of-order.
         */
        if (!hasRecord) {
            ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);

            if (status == ConsumeStatus.OK) {
                return ConsumeStatus.OK;
            }
        }

        consumerRepository.storeRecord(ConsumerUtils.mapRecord(record, keySerializer, valueSerializer));
        return ConsumeStatus.OK;
    }

}
