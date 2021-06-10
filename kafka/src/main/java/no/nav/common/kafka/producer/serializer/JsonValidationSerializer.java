package no.nav.common.kafka.producer.serializer;

import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.serialization.Serializer;
import org.everit.json.schema.Schema;
import org.json.JSONObject;

public class JsonValidationSerializer<T> implements Serializer<T> {

    private final Schema schema;

    public JsonValidationSerializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        String jsonStr = JsonUtils.toJson(data);

        validateJson(jsonStr);

        return jsonStr.getBytes();
    }

    private void validateJson(String jsonStr) {
        JSONObject jsonObject = new JSONObject(jsonStr);
        schema.validate(jsonObject);
    }

}
