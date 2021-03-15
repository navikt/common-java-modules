package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class KafkaProducerClientImplTest {

    @Test
    public void should_send_message_with_producer_async() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.send(record);

        assertEquals(record, mockProducer.history().get(0));
    }

    @Test
    public void should_send_message_with_producer_sync() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.sendSync(record);

        assertEquals(record, mockProducer.history().get(0));
    }

    @Test
    public void should_trigger_callback_with_exception_if_producer_is_closed() {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaProducerClientImpl<String, String> client = new KafkaProducerClientImpl<>(mockProducer);

        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        client.close();

        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        client.send(record, (metadata, exception) -> exceptionRef.set(exception));

        assertEquals(IllegalStateException.class, exceptionRef.get().getClass());
    }

}
