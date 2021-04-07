package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonTopicConsumerTest {

    @Test
    public void should_parse_json_record_before_consuming() {
        BiFunction<ConsumerRecord<String, String>, TestData, ConsumeStatus> consumer = (record, testData) -> {
            assertEquals(42, testData.test);
            return ConsumeStatus.OK;
        };

        JsonTopicConsumer<String, String, TestData> topicConsumer = new JsonTopicConsumer<>(TestData.class, consumer);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("test", 1, 1L, "1234", "{ \"test\": 42 }");

        topicConsumer.consume(record);
    }

    @Test
    public void should_pass_through_null_values() {
        BiFunction<ConsumerRecord<String, String>, TestData, ConsumeStatus> consumer = (record, testData) -> {
            assertNull(testData);
            return ConsumeStatus.OK;
        };

        JsonTopicConsumer<String, String, TestData> topicConsumer = new JsonTopicConsumer<>(TestData.class, consumer);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("test", 1, 1L, "1234", null);

        topicConsumer.consume(record);
    }

    public static class TestData {
        int test;
    }

}
