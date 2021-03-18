package no.nav.common.kafka.producer.util;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecord;
import no.nav.common.kafka.util.KafkaUtils;
import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Properties;

public class ProducerUtils {

    private ProducerUtils() {}

    public static KafkaProducerClient<byte[], byte[]> createByteProducer(String brokerUrl, Credentials credentials) {
        Properties properties = new Properties();
        return new KafkaProducerClientImpl<>(properties);
    }

    public static ProducerRecord<String, String> toJsonRecord(String topic, Object value) {
        return new ProducerRecord<>(topic, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toJsonRecord(String topic, String key, Object value) {
        return new ProducerRecord<>(topic, key, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toRecord(String topic, String key, String value) {
        return new ProducerRecord<>(topic, key, value);
    }

    public static <K, V> KafkaProducerRecord mapRecord(ProducerRecord<K, V> record, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        byte[] key = keySerializer.serialize(record.topic(), record.key());
        byte[] value = valueSerializer.serialize(record.topic(), record.value());
        String headersJson = KafkaUtils.headersToJson(record.headers());

        return new KafkaProducerRecord(record.topic(), key, value, headersJson);
    }

    public static ProducerRecord<byte[], byte[]> mapRecord(KafkaProducerRecord record) {
        ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(record.getTopic(), record.getKey(), record.getValue());

        Headers headers = KafkaUtils.jsonToHeaders(record.getHeadersJson());
        headers.forEach(header -> producerRecord.headers().add(header));

        return producerRecord;
    }

}
