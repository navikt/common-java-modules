package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class KafkaProducerClientImplTest {

    @Test
    public void should_send_message_with_producer() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.send(record);

        assertEquals(record, mockProducer.history().get(0));
    }

    @Test
    public void should_throw_exception_if_producer_is_closed() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.close();

        assertThrows(IllegalStateException.class, () -> client.send(record));
    }

}
