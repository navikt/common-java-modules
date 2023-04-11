package no.nav.common.kafka.spring;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord;
import no.nav.common.kafka.producer.util.ProducerUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KafkaProducerRecordStorageTest {

    @Test
    public void should_store_record_in_repository() {
        KafkaProducerRepository producerRepository = mock(KafkaProducerRepository.class);

        KafkaProducerRecordStorage producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("topic", "key", "value");
        producerRecord.headers().add(new RecordHeader("header", "header-value".getBytes()));

        producerRecordStorage.store(ProducerUtils.serializeStringRecord(producerRecord));

        ArgumentCaptor<StoredProducerRecord> recordCaptor = ArgumentCaptor.forClass(StoredProducerRecord.class);

        verify(producerRepository, times(1)).storeRecord(recordCaptor.capture());

        StoredProducerRecord storedRecord = recordCaptor.getValue();

        assertEquals("topic", storedRecord.getTopic());
        assertArrayEquals("key".getBytes(), storedRecord.getKey());
        assertArrayEquals("value".getBytes(), storedRecord.getValue());
        assertEquals("[{\"key\":\"header\",\"value\":\"aGVhZGVyLXZhbHVl\"}]", storedRecord.getHeadersJson());
    }

    @Test
    public void should_throw_exceptions_from_repository() {
        KafkaProducerRepository producerRepository = mock(KafkaProducerRepository.class);

        // Could be any exception
        when(producerRepository.storeRecord(any())).thenThrow(new IllegalStateException());

        KafkaProducerRecordStorage producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

        assertThrows(
                IllegalStateException.class,
                () -> producerRecordStorage.store(ProducerUtils.serializeStringRecord(new ProducerRecord<>("topic", "key", "value")))
        );
    }

}
