package no.nav.common.kafka.producer.serializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.common.utils.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonSerializerTest {

    @Test
    public void should_serialize_json_value() {
        JsonSerializer<TestData> serializer = new JsonSerializer<>();
        TestData testData = new TestData("test");
        assertEquals(new Bytes(JsonUtils.toJson(testData).getBytes()), new Bytes(serializer.serialize("topic", testData)));
    }

    @Test
    public void should_handle_null_value() {
        JsonSerializer<TestData> serializer = new JsonSerializer<>();
        assertNull(serializer.serialize("topic", null));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestData {
        String test;
    }

}
