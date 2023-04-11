package no.nav.common.kafka.producer.serializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.utils.Bytes;
import org.everit.json.schema.Schema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class JsonValidationSerializerTest {

    @Test
    public void should_validate_and_serialize_object() {
        Schema schema = mock(Schema.class);

        TestData testData = new TestData("Test data");
        byte[] testDataBytes = JsonUtils.toJson(testData).getBytes();

        JsonValidationSerializer<TestData> serializer = new JsonValidationSerializer<>(schema);
        byte[] serializedBytes = serializer.serialize("topic", testData);

        verify(schema, times(1)).validate(any());
        assertEquals(new Bytes(serializedBytes), new Bytes(testDataBytes));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestData {
        String test;
    }

}
