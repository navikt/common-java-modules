package no.nav.common.kafka.producer.util;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord;
import no.nav.common.kafka.util.KafkaUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

public class ProducerUtils {

    private ProducerUtils() {}

    public static ProducerRecord<String, String> toJsonProducerRecord(String topic, Object value) {
        return new ProducerRecord<>(topic, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toJsonProducerRecord(String topic, String key, Object value) {
        return new ProducerRecord<>(topic, key, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toProducerRecord(String topic, String key, String value) {
        return new ProducerRecord<>(topic, key, value);
    }

    public static <K, V> StoredProducerRecord mapToStoredRecord(ProducerRecord<K, V> record, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        byte[] key = keySerializer.serialize(record.topic(), record.key());
        byte[] value = valueSerializer.serialize(record.topic(), record.value());
        String headersJson = KafkaUtils.headersToJson(record.headers());

        return new StoredProducerRecord(record.topic(), key, value, headersJson);
    }

    public static ProducerRecord<byte[], byte[]> mapFromStoredRecord(StoredProducerRecord record) {
        Headers headers = KafkaUtils.jsonToHeaders(record.getHeadersJson());

        return new ProducerRecord<>(
                record.getTopic(),
                null,
                null,
                record.getKey(),
                record.getValue(),
                headers
        );
    }

}
