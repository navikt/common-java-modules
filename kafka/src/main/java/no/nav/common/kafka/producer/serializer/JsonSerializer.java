package no.nav.common.kafka.producer.serializer;

import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        return JsonUtils.toJson(data).getBytes();
    }

}
