package no.nav.common.kafka.consumer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

public class ConsumerUtilsTest {

    @Test
    public void safeConsume__should_return_failed_when_underlying_consumer_returns_null() {
        TopicConsumer<String, String> consumer = record -> null;

        ConsumerRecord<String, String> record = new ConsumerRecord<>("", 1, 1L, "", "");

        assertEquals(ConsumeStatus.FAILED, ConsumerUtils.safeConsume(consumer, record));
    }
}
