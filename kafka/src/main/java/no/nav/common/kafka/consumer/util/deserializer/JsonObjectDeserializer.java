package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;

import static no.nav.common.json.JsonUtils.fromJson;

public class JsonObjectDeserializer<T> implements Deserializer<T> {

    private final Class<T> dataClass;

    public JsonObjectDeserializer(Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        String dataStr = new String(data, StandardCharsets.UTF_8);

        return fromJson(dataStr, dataClass);
    }

}
