package no.nav.common.kafka.consumer.deserializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.consumer.util.deserializer.JsonValidationDeserializer;
import org.everit.json.schema.Schema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JsonValidationDeserializerTest {

    @Test
    public void should_validate_and_deserialize_object() {
        Schema schema = mock(Schema.class);

        TestData testData = new TestData("Test data");
        byte[] testDataBytes = JsonUtils.toJson(testData).getBytes();

        JsonValidationDeserializer<TestData> serializer = new JsonValidationDeserializer<>(schema, TestData.class);
        TestData deserializedTestData = serializer.deserialize("topic", testDataBytes);

        verify(schema, times(1)).validate(any());
        assertEquals(testData, deserializedTestData);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestData {
        String test;
    }

}
