package no.nav.common.kafka.producer.feilhandtering;

import static no.nav.common.kafka.util.WorkerLifecycleTest.ryddExecutor;
import static no.nav.common.kafka.util.WorkerLifecycleTest.vent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import no.nav.common.kafka.producer.feilhandtering.publisher.KafkaProducerRecordPublisher;
import org.junit.Test;

public class KafkaProducerRecordProcessorLifecycleTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final long DAY_MS = Duration.ofDays(1).toMillis();

    @Test
    @SuppressWarnings("removal")
    public void skal_bevare_ikke_blokkerende_close_og_fullfore_publisering_for_lukking() throws Exception {
        assertTrue(KafkaProducerRecordProcessor.class
                .getMethod("close")
                .getAnnotation(Deprecated.class)
                .forRemoval());
        KafkaProducerRepository repository = mock(KafkaProducerRepository.class);
        KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
        List<StoredProducerRecord> records = List.of(mock(StoredProducerRecord.class));
        CountDownLatch publishing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(repository.getRecords(10)).thenReturn(records);
        when(publisher.publishStoredRecords(records)).thenAnswer(invocation -> {
            publishing.countDown();
            vent(release);
            assertFalse(Thread.currentThread().isInterrupted());
            return List.of(1L);
        });
        KafkaProducerRecordProcessor processor = processor(repository, publisher);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        Throwable failure = null;
        try {
            processor.start();
            vent(publishing);
            Future<?> closing = caller.submit(processor::close);
            closing.get(1, TimeUnit.SECONDS);
            processor.close();
            verify(repository, never()).deleteRecords(List.of(1L));
            verify(publisher, never()).close();
            assertThrows(IllegalStateException.class, processor::start);
            assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(Duration.ZERO));
            release.countDown();
            processor.closeAndAwait(TIMEOUT);
            verify(repository).deleteRecords(List.of(1L));
            verify(publisher).close();
            processor.close();
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(caller, failure, () -> {
                release.countDown();
                processor.closeAndAwait(TIMEOUT);
            });
        }
    }

    @Test
    public void skal_lukke_publisher_en_gang_selv_uten_oppstart() throws Exception {
        KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
        KafkaProducerRecordProcessor processor = processor(mock(KafkaProducerRepository.class), publisher);
        processor.closeAndAwait();
        processor.closeAndAwait(TIMEOUT);
        verify(publisher, times(1)).close();
        assertThrows(IllegalStateException.class, processor::start);
    }

    @Test
    public void skal_vente_pa_publisering_sletting_og_publisherlukking_uten_interrupt() throws Exception {
        KafkaProducerRepository repository = mock(KafkaProducerRepository.class);
        KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
        StoredProducerRecord storedRecord = mock(StoredProducerRecord.class);
        List<StoredProducerRecord> records = List.of(storedRecord);
        CountDownLatch publishing = new CountDownLatch(1);
        CountDownLatch publishRelease = new CountDownLatch(1);
        CountDownLatch deleting = new CountDownLatch(1);
        CountDownLatch deleteRelease = new CountDownLatch(1);
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch closeRelease = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        when(repository.getRecords(10)).thenReturn(records);
        when(publisher.publishStoredRecords(records)).thenAnswer(invocation -> {
            publishing.countDown();
            vent(publishRelease);
            interrupted.set(Thread.currentThread().isInterrupted());
            return List.of(1L);
        });
        doAnswer(invocation -> {
                    deleting.countDown();
                    vent(deleteRelease);
                    return null;
                })
                .when(repository)
                .deleteRecords(List.of(1L));
        doAnswer(invocation -> {
                    closing.countDown();
                    vent(closeRelease);
                    return null;
                })
                .when(publisher)
                .close();
        KafkaProducerRecordProcessor processor = processor(repository, publisher);
        try {
            processor.start();
            processor.start();
            vent(publishing);
            assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(Duration.ZERO));
            publishRelease.countDown();
            vent(deleting);
            assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(Duration.ZERO));
            deleteRelease.countDown();
            vent(closing);
            assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(Duration.ZERO));
            closeRelease.countDown();
            processor.closeAndAwait(TIMEOUT);
            verify(publisher, times(1)).publishStoredRecords(records);
            verify(repository, times(1)).deleteRecords(List.of(1L));
            verify(publisher, times(1)).close();
            assertFalse(interrupted.get());
        } finally {
            publishRelease.countDown();
            deleteRelease.countDown();
            closeRelease.countDown();
            processor.closeAndAwait(TIMEOUT);
        }
    }

    @Test
    @SuppressWarnings("removal")
    public void skal_melde_publisherfeil_ved_gjentatt_lukking() throws Exception {
        KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
        doThrow(new IOException("Simulert publisherfeil")).when(publisher).close();
        KafkaProducerRecordProcessor processor = processor(mock(KafkaProducerRepository.class), publisher);
        processor.close();
        assertThrows(IllegalStateException.class, processor::closeAndAwait);
        processor.close();
        assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(TIMEOUT));
        verify(publisher, times(1)).close();
    }

    @Test
    public void skal_lukke_umiddelbart_etter_start_uten_a_gjenopplive_arbeideren() throws Exception {
        for (int i = 0; i < 20; i++) {
            KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
            KafkaProducerRecordProcessor processor = processor(mock(KafkaProducerRepository.class), publisher);
            processor.start();
            processor.closeAndAwait(TIMEOUT);
            verify(publisher).close();
            assertThrows(IllegalStateException.class, processor::start);
        }
    }

    @Test
    @SuppressWarnings("removal")
    public void skal_avvise_selvventing_fra_publisher_og_fullfore_databasesletting() throws Exception {
        KafkaProducerRepository repository = mock(KafkaProducerRepository.class);
        KafkaProducerRecordPublisher publisher = mock(KafkaProducerRecordPublisher.class);
        List<StoredProducerRecord> records = List.of(mock(StoredProducerRecord.class));
        when(repository.getRecords(10)).thenReturn(records);
        CountDownLatch callback = new CountDownLatch(1);
        KafkaProducerRecordProcessor processor = processor(repository, publisher);
        when(publisher.publishStoredRecords(records)).thenAnswer(invocation -> {
            processor.close();
            assertThrows(IllegalStateException.class, () -> processor.closeAndAwait(TIMEOUT));
            callback.countDown();
            return List.of(1L);
        });
        try {
            processor.start();
            vent(callback);
            processor.closeAndAwait(TIMEOUT);
            verify(repository).deleteRecords(List.of(1L));
            verify(publisher).close();
        } finally {
            processor.closeAndAwait(TIMEOUT);
        }
    }

    private static KafkaProducerRecordProcessor processor(
            KafkaProducerRepository repository, KafkaProducerRecordPublisher publisher) {
        return new KafkaProducerRecordProcessor(
                DAY_MS, DAY_MS, DAY_MS, 10, false, repository, publisher, () -> true, null);
    }
}
