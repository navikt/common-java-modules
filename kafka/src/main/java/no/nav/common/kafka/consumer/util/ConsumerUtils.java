package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import no.nav.common.kafka.util.KafkaUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;

public class ConsumerUtils {

    public final static Serializer<byte[]> BYTE_ARRAY_SERIALIZER = new ByteArraySerializer();

    private final static Logger log = LoggerFactory.getLogger(ConsumerUtils.class);

    public static StoredConsumerRecord mapToStoredRecord(ConsumerRecord<byte[], byte[]> record) {
        return mapToStoredRecord(record, BYTE_ARRAY_SERIALIZER, BYTE_ARRAY_SERIALIZER);
    }

    public static <K, V> StoredConsumerRecord mapToStoredRecord(
            ConsumerRecord<K, V> record,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer
    ) {
        byte[] key = keySerializer.serialize(record.topic(), record.key());
        byte[] value = valueSerializer.serialize(record.topic(), record.value());
        String headersJson = KafkaUtils.headersToJson(record.headers());

        return new StoredConsumerRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                key,
                value,
                headersJson,
                record.timestamp()
        );
    }

    public static ConsumerRecord<byte[], byte[]> mapFromStoredRecord(StoredConsumerRecord record) {
        Headers headers = KafkaUtils.jsonToHeaders(record.getHeadersJson());

        ConsumerRecord<byte[], byte[]> consumerRecord = new ConsumerRecord<>(
                record.getTopic(),
                record.getPartition(),
                record.getOffset(),
                record.getTimestamp(),
                TimestampType.CREATE_TIME,
                ConsumerRecord.NULL_CHECKSUM,
                ConsumerRecord.NULL_SIZE,
                ConsumerRecord.NULL_SIZE,
                record.getKey(),
                record.getValue()
        );

        headers.forEach(header -> consumerRecord.headers().add(header));

        return consumerRecord;
    }

    public static <K, V> ConsumerRecord<K, V> deserializeConsumerRecord(
            ConsumerRecord<byte[], byte[]> record,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {
        K key = keyDeserializer.deserialize(record.topic(), record.key());
        V value = valueDeserializer.deserialize(record.topic(), record.value());

        return new ConsumerRecord<>(
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                record.timestampType(),
                ConsumerRecord.NULL_CHECKSUM,
                ConsumerRecord.NULL_SIZE,
                ConsumerRecord.NULL_SIZE,
                key,
                value
        );
    }

    public static Map<String, TopicConsumer<byte[], byte[]>> createTopicConsumers(List<TopicConsumerConfig<?, ?>> topicConsumerConfigs) {
        Map<String, TopicConsumer<byte[], byte[]>> consumers = new HashMap<>();
        topicConsumerConfigs.forEach(config -> consumers.put(config.getTopic(), createTopicConsumer(config)));
        return consumers;
    }

    public static <K, V> TopicConsumer<byte[], byte[]> createTopicConsumer(TopicConsumerConfig<K, V> config) {
        return record -> {
            ConsumerRecord<K, V> deserializedRecord = deserializeConsumerRecord(
                    record,
                    config.getKeyDeserializer(),
                    config.getValueDeserializer()
            );

            return config.getConsumer().consume(deserializedRecord);
        };
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

    /**
     * Used to wrap consumers that dont return a ConsumeStatus
     * @param consumer the consumer which will consume the record
     * @param record the kafka record to consume
     * @param <K> topic key
     * @param <V> topic value
     * @return ConsumeStatus.OK
     */
    public static <K, V> ConsumeStatus consume(Consumer<ConsumerRecord<K, V>> consumer, ConsumerRecord<K, V> record) {
        consumer.accept(record);
        return ConsumeStatus.OK;
    }

    public static <K, V> ConsumeStatus safeConsume(TopicConsumer<K, V> topicConsumer, ConsumerRecord<K, V> consumerRecord) {
        try {
            ConsumeStatus status = topicConsumer.consume(consumerRecord);

            if (status == null) {
                log.warn(
                        "Consumer returned null instead of OK/FAILED, defaulting to FAILED. topic={} partition={} offset={}",
                        consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset()
                );
                return ConsumeStatus.FAILED;
            }

            return status;
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
