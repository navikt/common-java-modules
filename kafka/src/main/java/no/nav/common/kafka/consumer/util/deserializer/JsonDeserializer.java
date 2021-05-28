package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.Deserializer;

public class JsonDeserializer<T> implements Deserializer<T> {

    private final Class<T> dataClass;

    public JsonDeserializer(Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        // TODO: implement
        return null;
    }

}
