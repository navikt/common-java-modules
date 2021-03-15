package no.nav.common.kafka.producer.util;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerUtils {

    private ProducerUtils() {}

    public static ProducerRecord<String, String> toJsonRecord(String topic, Object value) {
        return new ProducerRecord<>(topic, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toJsonRecord(String topic, String key, Object value) {
        return new ProducerRecord<>(topic, key, JsonUtils.toJson(value));
    }

    public static ProducerRecord<String, String> toRecord(String topic, String key, String value) {
        return new ProducerRecord<>(topic, key, value);
    }

    public static <K, V> ProducerRecord<K, V> mapRecord(KafkaProducerRecord<K, V> record) {
        return new ProducerRecord<>(record.getTopic(), record.getKey(), record.getValue());
    }

}
