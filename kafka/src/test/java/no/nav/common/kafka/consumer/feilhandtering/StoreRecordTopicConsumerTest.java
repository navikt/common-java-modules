package no.nav.common.kafka.consumer.feilhandtering;

import no.nav.common.kafka.consumer.ConsumeStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class StoreRecordTopicConsumerTest {

    @Test
    public void should_store_record() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);

        StoreRecordTopicConsumer storeRecordTopicConsumer = new StoreRecordTopicConsumer(consumerRepository);

        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>("topic", 1, 1, "key".getBytes(), "value".getBytes());

        assertEquals(ConsumeStatus.OK, storeRecordTopicConsumer.consume(record));
        verify(consumerRepository, times(1)).storeRecord(any());
    }

    @Test
    public void should_return_failed_status_when_store_fails() {
        KafkaConsumerRepository consumerRepository = mock(KafkaConsumerRepository.class);

        when(consumerRepository.storeRecord(any())).thenThrow(new RuntimeException());

        StoreRecordTopicConsumer storeRecordTopicConsumer = new StoreRecordTopicConsumer(consumerRepository);

        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>("topic", 1, 1, "key".getBytes(), "value".getBytes());

        assertEquals(ConsumeStatus.FAILED, storeRecordTopicConsumer.consume(record));
    }


}
