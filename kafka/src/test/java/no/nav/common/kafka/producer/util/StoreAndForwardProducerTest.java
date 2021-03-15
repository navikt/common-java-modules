package no.nav.common.kafka.producer.util;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.StoreAndForwardProducer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StoreAndForwardProducerTest {

    @Test
    public void should_store_message() {
        KafkaProducerRepository<String, String> repository = mock(KafkaProducerRepository.class);
        KafkaProducerClient<String, String> client = mock(KafkaProducerClient.class);
        StoreAndForwardProducer<String, String> producer = new StoreAndForwardProducer<>(repository, client);

        when(repository.storeRecord(any())).thenReturn(1L);

        ProducerRecord<String, String> record = ProducerUtils.toRecord("topic", "key", "value");

        producer.send(ProducerUtils.toRecord("topic", "key", "value"));

        verify(repository, times(1)).storeRecord(record);
    }

    @Test
    public void should_delete_message_after_sending_succeeded() {
        KafkaProducerRepository<String, String> repository = mock(KafkaProducerRepository.class);
        KafkaProducerClient<String, String> client = mock(KafkaProducerClient.class);
        StoreAndForwardProducer<String, String> producer = new StoreAndForwardProducer<>(repository, client);

        ArgumentCaptor<Callback> captor = ArgumentCaptor.forClass(Callback.class);

        when(repository.storeRecord(any())).thenReturn(1L);

        producer.send(ProducerUtils.toRecord("topic", "key", "value"));

        verify(client, times(1)).send(any(), captor.capture());

        captor.getValue().onCompletion(null, new Exception());

        verify(repository, never()).deleteRecord(1L);
    }

    @Test
    public void should_not_delete_message_after_sending_failed() {
        KafkaProducerRepository<String, String> repository = mock(KafkaProducerRepository.class);
        KafkaProducerClient<String, String> client = mock(KafkaProducerClient.class);
        StoreAndForwardProducer<String, String> producer = new StoreAndForwardProducer<>(repository, client);

        ArgumentCaptor<Callback> captor = ArgumentCaptor.forClass(Callback.class);

        when(repository.storeRecord(any())).thenReturn(1L);

        producer.send(ProducerUtils.toRecord("topic", "key", "value"));

        verify(client, times(1)).send(any(), captor.capture());

        captor.getValue().onCompletion(null, null);

        verify(repository, times(1)).deleteRecord(1L);
    }


}
