package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.nav.common.json.JsonUtils.fromJsonArray;

public class JsonArrayDeserializer<T> implements Deserializer<List<T>> {

    private final Class<T> dataClass;

    public JsonArrayDeserializer(Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public List<T> deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        String dataStr = new String(data, StandardCharsets.UTF_8);

        return fromJsonArray(dataStr, dataClass);
    }

}
