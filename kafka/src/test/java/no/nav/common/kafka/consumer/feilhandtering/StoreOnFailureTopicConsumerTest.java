package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StoreOnFailureTopicConsumerTest {

    @Test
    public void should_not_store_when_key_is_not_stored_and_consumption_succeeded() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);
        TopicConsumer<String, String> consumer = mock(TopicConsumer.class);

        StoreOnFailureTopicConsumer<String, String> storeOnFailureConsumer = new StoreOnFailureTopicConsumer<>(
                consumer,
                consumerRepository,
                new StringSerializer(),
                new StringSerializer()
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");

        when(consumerRepository.hasRecordWithKey(any(), anyInt(), any())).thenReturn(false);
        when(consumer.consume(any())).thenReturn(ConsumeStatus.OK);

        assertEquals(ConsumeStatus.OK, storeOnFailureConsumer.consume(record));

        verify(consumer, times(1)).consume(any());
        verify(consumerRepository, never()).storeRecord(any());
    }

    @Test
    public void should_store_when_has_key_in_database() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);
        TopicConsumer<String, String> consumer = mock(TopicConsumer.class);

        StoreOnFailureTopicConsumer<String, String> storeOnFailureConsumer = new StoreOnFailureTopicConsumer<>(
                consumer,
                consumerRepository,
                new StringSerializer(),
                new StringSerializer()
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");

        when(consumerRepository.hasRecordWithKey(any(), anyInt(), any())).thenReturn(true);
        when(consumer.consume(any())).thenReturn(ConsumeStatus.OK);

        assertEquals(ConsumeStatus.OK, storeOnFailureConsumer.consume(record));

        verify(consumer, never()).consume(any());
        verify(consumerRepository, times(1)).storeRecord(any());
    }

    @Test
    public void should_store_when_consumer_returns_failed_status() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);
        TopicConsumer<String, String> consumer = mock(TopicConsumer.class);

        StoreOnFailureTopicConsumer<String, String> storeOnFailureConsumer = new StoreOnFailureTopicConsumer<>(
                consumer,
                consumerRepository,
                new StringSerializer(),
                new StringSerializer()
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");

        when(consumerRepository.hasRecordWithKey(any(), anyInt(), any())).thenReturn(false);
        when(consumer.consume(any())).thenReturn(ConsumeStatus.FAILED);

        assertEquals(ConsumeStatus.OK, storeOnFailureConsumer.consume(record));

        verify(consumer, times(1)).consume(any());
        verify(consumerRepository, times(1)).storeRecord(any());
    }

    @Test
    public void should_store_when_consumer_throws_exception() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);
        TopicConsumer<String, String> consumer = mock(TopicConsumer.class);

        StoreOnFailureTopicConsumer<String, String> storeOnFailureConsumer = new StoreOnFailureTopicConsumer<>(
                consumer,
                consumerRepository,
                new StringSerializer(),
                new StringSerializer()
        );

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");

        when(consumerRepository.hasRecordWithKey(any(), anyInt(), any())).thenReturn(false);
        when(consumer.consume(any())).thenThrow(new RuntimeException());

        assertEquals(ConsumeStatus.OK, storeOnFailureConsumer.consume(record));

        verify(consumer, times(1)).consume(any());
        verify(consumerRepository, times(1)).storeRecord(any());
    }

}
