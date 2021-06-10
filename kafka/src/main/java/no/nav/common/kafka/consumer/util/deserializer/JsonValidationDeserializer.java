package no.nav.common.kafka.consumer.util.deserializer;

import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.everit.json.schema.Schema;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class JsonValidationDeserializer<T> implements Deserializer<T> {

    private final Schema schema;

    private final Class<T> dataClass;

    public JsonValidationDeserializer(Schema schema, Class<T> dataClass) {
        this.schema = schema;
        this.dataClass = dataClass;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        String jsonStr = new String(data, StandardCharsets.UTF_8);

        validateJson(jsonStr);

        return JsonUtils.fromJson(jsonStr, dataClass);
    }

    private void validateJson(String jsonStr) {
        JSONObject jsonObject = new JSONObject(jsonStr);
        schema.validate(jsonObject);
    }

}
