package no.nav.common.kafka.producer.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.utils.Bytes;
import org.everit.json.schema.Schema;
import org.junit.jupiter.api.Test;

public class JsonValidationSerializerTest {

    @Test
    public void should_validate_and_serialize_object() {
        Schema schema = mock(Schema.class);

        TestData testData = new TestData("Test data");
        byte[] testDataBytes = JsonUtils.toJson(testData).getBytes();

        try (JsonValidationSerializer<TestData> serializer = new JsonValidationSerializer<>(schema)) {
            byte[] serializedBytes = serializer.serialize("topic", testData);

            verify(schema, times(1)).validate(any());
            assertEquals(new Bytes(testDataBytes), new Bytes(serializedBytes));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestData {
        String test;
    }
}
