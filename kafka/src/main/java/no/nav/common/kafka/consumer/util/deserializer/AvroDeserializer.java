package no.nav.common.kafka.consumer.util.deserializer;

import org.apache.kafka.common.serialization.Deserializer;

public class AvroDeserializer implements Deserializer<Object> {

    // TODO: Take SchemaRegistryClient
    public AvroDeserializer() {}

    @Override
    public Object deserialize(String topic, byte[] data) {
        // TODO: implement
        return null;
    }

}
