package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecord;
import no.nav.common.kafka.consumer.feilhandtering.StoredRecordConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class ConsumerUtils {

    private final static Logger log = LoggerFactory.getLogger(ConsumerUtils.class);

    public static <K, V> KafkaConsumerRecord mapRecord(
            ConsumerRecord<K, V> record,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer
    ) {
        byte[] key = keySerializer.serialize(record.topic(), record.key());
        byte[] value = valueSerializer.serialize(record.topic(), record.value());

        return new KafkaConsumerRecord(record.topic(), record.partition(), record.offset(), key, value);
    }

    public static KafkaConsumerRecord mapRecord(ConsumerRecord<byte[], byte[]> record) {
        return new KafkaConsumerRecord(record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static <K, V> ConsumerRecord<K, V> mapRecord(
            KafkaConsumerRecord record,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {
        K key = keyDeserializer.deserialize(record.getTopic(), record.getKey());
        V value = valueDeserializer.deserialize(record.getTopic(), record.getValue());

        return new ConsumerRecord<>(record.getTopic(), record.getPartition(), record.getOffset(), key, value);
    }

    public static <K, V> Map<String, StoredRecordConsumer> toStoredRecordConsumerMap(
            Map<String, TopicConsumer<K, V>> consumerMap,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {
        Map<String, StoredRecordConsumer> storedRecordConsumerMap = new HashMap<>();

        consumerMap.forEach((topic, topicConsumer) -> {
            storedRecordConsumerMap.put(topic, toStoredRecordConsumer(topicConsumer, keyDeserializer, valueDeserializer));
        });

        return storedRecordConsumerMap;
    }

    public static <K, V> StoredRecordConsumer toStoredRecordConsumer(
            TopicConsumer<K, V> topicConsumer,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {
        return storedRecord -> topicConsumer.consume(mapRecord(storedRecord, keyDeserializer, valueDeserializer));
    }

    public static <K, V> TopicConsumer<K, V> aggregateConsumer(final List<TopicConsumer<K, V>> consumers) {
        return record -> {
            ConsumeStatus aggregatedStatus = ConsumeStatus.OK;

            for (TopicConsumer<K, V> consumer : consumers) {
                ConsumeStatus status = consumer.consume(record);

                if (status == ConsumeStatus.FAILED) {
                    aggregatedStatus = ConsumeStatus.FAILED;
                }
            }

            return aggregatedStatus;
        };
    }

    public static <K, V> ConsumeStatus safeConsume(TopicConsumer<K, V> topicConsumer, ConsumerRecord<K, V> consumerRecord) {
        try {
            return topicConsumer.consume(consumerRecord);
        } catch (Exception e) {
            String msg = format(
                    "Consumer failed to process record from topic=%s partition=%d offset=%d",
                    consumerRecord.topic(),
                    consumerRecord.partition(),
                    consumerRecord.offset()
            );

            log.error(msg, e);
            return ConsumeStatus.FAILED;
        }
    }

}
