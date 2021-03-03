package no.nav.common.kafka.producer;

import no.nav.common.json.JsonUtils;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerUtils {

    public static Properties kafkaProducerDefaultProperties() {
        return null;
    }

    public static KafkaProducerClientConfig config() {
        return new KafkaProducerClientConfig();
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


}
