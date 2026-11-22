package no.nav.common.kafka.consumer.feilhandtering;

import static no.nav.common.kafka.util.WorkerLifecycleTest.shutdownHook;
import static no.nav.common.kafka.util.WorkerLifecycleTest.vent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.Test;

public class KafkaConsumerRecordProcessorLifecycleTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final TopicPartition PARTITION = new TopicPartition("topic", 0);

    @Test
    public void skal_bevare_standardhook_og_kunne_velge_hook_i_builderen() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            KafkaConsumerRecordProcessorBuilder builder = KafkaConsumerRecordProcessorBuilder.builder()
                    .withLockProvider(configuration -> Optional.of(() -> {}))
                    .withKafkaConsumerRepository(mock(KafkaConsumerRepository.class))
                    .withConsumerConfigs(List.of(new TopicConsumerConfig<>(
                            "topic",
                            new ByteArrayDeserializer(),
                            new ByteArrayDeserializer(),
                            consumerRecord -> ConsumeStatus.OK)));
            if (mode == 1) {
                builder = builder.withShutdownHookEnabled(false).withShutdownHookEnabled(true);
            } else if (mode == 2) {
                builder = builder.withShutdownHookEnabled(false);
            }
            KafkaConsumerRecordProcessor processor = builder.build();
            Thread hook;
            try (processor) {
                hook = shutdownHook(processor);
                assertEquals(mode != 2, hook != null);
            }
            if (hook != null) {
                assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
            }
            assertThrows(IllegalStateException.class, processor::start);
        }
    }

    @Test
    public void skal_bevare_hook_i_gammel_konstruktor_og_tillate_avslatt_hook_i_ny() throws Exception {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        try (KafkaConsumerRecordProcessor processor = processor(repository, consumerRecord -> ConsumeStatus.OK, null)) {
            assertNotNull(shutdownHook(processor));
        }
        try (KafkaConsumerRecordProcessor processor = new KafkaConsumerRecordProcessor(
                configuration -> Optional.of(() -> {}),
                repository,
                Map.of("topic", consumerRecord -> ConsumeStatus.OK),
                new KafkaConsumerRecordProcessorConfig(
                        Duration.ofDays(1), Duration.ofDays(1), 10, storedRecord -> Duration.ZERO),
                null,
                false)) {
            assertNull(shutdownHook(processor));
        }
    }

    @Test
    public void skal_fullfore_batch_og_sletting_uten_shutdown_hook() throws Exception {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        CountDownLatch deleted = new CountDownLatch(1);
        when(repository.getTopicPartitions(anyList())).thenReturn(List.of(PARTITION));
        when(repository.getRecords("topic", 0, 10)).thenReturn(List.of(storedRecord()));
        doAnswer(invocation -> {
                    deleted.countDown();
                    return null;
                })
                .when(repository)
                .deleteRecords(List.of(1L));
        try (KafkaConsumerRecordProcessor processor = new KafkaConsumerRecordProcessor(
                configuration -> Optional.of(() -> {}),
                repository,
                Map.of("topic", consumerRecord -> ConsumeStatus.OK),
                new KafkaConsumerRecordProcessorConfig(
                        Duration.ofDays(1), Duration.ofDays(1), 10, storedRecord -> Duration.ZERO),
                null,
                false)) {
            assertNull(shutdownHook(processor));
            processor.start();
            vent(deleted);
            processor.close(TIMEOUT);
            verify(repository).deleteRecords(List.of(1L));
            assertThrows(IllegalStateException.class, processor::start);
        }
    }

    @Test
    public void skal_bevare_rekkefolge_backoff_og_sletting_i_en_partisjonsbatch() {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        List<Long> consumed = new ArrayList<>();
        CountDownLatch deleted = new CountDownLatch(1);
        when(repository.getTopicPartitions(anyList())).thenReturn(List.of(PARTITION));
        when(repository.getRecords("topic", 0, 10))
                .thenReturn(List.of(
                        storedRecord(1, new byte[] {1}),
                        storedRecord(2, new byte[] {1}),
                        storedRecord(3, new byte[] {2}),
                        storedRecord(4, new byte[] {2}),
                        storedRecord(5, new byte[] {3}),
                        storedRecord(6, null),
                        storedRecord(7, null)))
                .thenReturn(List.of());
        doAnswer(invocation -> {
                    deleted.countDown();
                    return null;
                })
                .when(repository)
                .deleteRecords(List.of(5L, 7L));
        try (KafkaConsumerRecordProcessor processor = new KafkaConsumerRecordProcessor(
                configuration -> Optional.of(() -> {}),
                repository,
                Map.of("topic", consumerRecord -> {
                    consumed.add(consumerRecord.offset());
                    if (consumerRecord.offset() == 6) {
                        throw new IllegalStateException("Simulert behandlingsfeil");
                    }
                    return consumerRecord.offset() == 1 ? ConsumeStatus.FAILED : ConsumeStatus.OK;
                }),
                new KafkaConsumerRecordProcessorConfig(
                        Duration.ofDays(1),
                        Duration.ofDays(1),
                        10,
                        storedRecord -> storedRecord.getId() == 3 ? Duration.ofDays(1) : Duration.ZERO),
                null)) {
            processor.start();
            vent(deleted);
            processor.close(TIMEOUT);
            assertEquals(List.of(1L, 5L, 6L, 7L), consumed);
            verify(repository).incrementRetries(1L);
            verify(repository).incrementRetries(6L);
            verify(repository, never()).incrementRetries(2L);
            verify(repository, never()).incrementRetries(3L);
            verify(repository, never()).incrementRetries(4L);
            verify(repository).deleteRecords(List.of(5L, 7L));
        }
    }

    @Test
    public void skal_vente_pa_callback_databasesletting_og_metrikkarbeider() {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        CountDownLatch consuming = new CountDownLatch(1);
        CountDownLatch consumeRelease = new CountDownLatch(1);
        CountDownLatch deleting = new CountDownLatch(1);
        CountDownLatch deleteRelease = new CountDownLatch(1);
        CountDownLatch metrics = new CountDownLatch(1);
        CountDownLatch metricsRelease = new CountDownLatch(1);
        AtomicInteger consumed = new AtomicInteger();
        when(repository.getTopicPartitions(anyList())).thenReturn(List.of(PARTITION));
        when(repository.getRecords("topic", 0, 10)).thenReturn(List.of(storedRecord()));
        when(repository.getFailedRecordCounts(anyList())).thenAnswer(invocation -> {
            metrics.countDown();
            vent(metricsRelease);
            return Map.of(PARTITION, 1L);
        });
        doAnswer(invocation -> {
                    deleting.countDown();
                    vent(deleteRelease);
                    return null;
                })
                .when(repository)
                .deleteRecords(List.of(1L));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            KafkaConsumerRecordProcessor processor = processor(
                    repository,
                    consumerRecord -> {
                        consumed.incrementAndGet();
                        consuming.countDown();
                        vent(consumeRelease);
                        return ConsumeStatus.OK;
                    },
                    registry);
            try {
                processor.start();
                processor.start();
                vent(consuming);
                vent(metrics);
                assertThrows(IllegalStateException.class, () -> processor.close(Duration.ZERO));
                consumeRelease.countDown();
                vent(deleting);
                assertThrows(IllegalStateException.class, () -> processor.close(Duration.ZERO));
                deleteRelease.countDown();
                assertThrows(IllegalStateException.class, () -> processor.close(Duration.ZERO));
                metricsRelease.countDown();
                processor.close(TIMEOUT);
                verify(repository).deleteRecords(List.of(1L));
                assertEquals(1, consumed.get());
                assertThrows(IllegalStateException.class, processor::start);
            } finally {
                consumeRelease.countDown();
                deleteRelease.countDown();
                metricsRelease.countDown();
                processor.close(TIMEOUT);
            }
        } finally {
            registry.close();
        }
    }

    @Test
    public void skal_stoppe_uten_a_vente_og_starte_igjen_etter_fullfort_batch() {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch deleted = new CountDownLatch(1);
        when(repository.getTopicPartitions(anyList())).thenReturn(List.of(PARTITION));
        when(repository.getRecords("topic", 0, 10))
                .thenReturn(List.of(storedRecord()))
                .thenReturn(List.of());
        doAnswer(invocation -> {
                    deleted.countDown();
                    return null;
                })
                .when(repository)
                .deleteRecords(List.of(1L));
        KafkaConsumerRecordProcessor processor = processor(
                repository,
                consumerRecord -> {
                    entered.countDown();
                    vent(release);
                    return ConsumeStatus.OK;
                },
                null);
        try {
            processor.start();
            vent(entered);
            processor.stop();
            release.countDown();
            vent(deleted);
            processor.start();
            processor.stop();
            processor.close(TIMEOUT);
            verify(repository).deleteRecords(List.of(1L));
        } finally {
            release.countDown();
            processor.close(TIMEOUT);
        }
    }

    @Test
    public void skal_lukke_for_oppstart_og_flere_ganger() {
        KafkaConsumerRecordProcessor processor =
                processor(mock(KafkaConsumerRepository.class), consumerRecord -> ConsumeStatus.OK, null);
        processor.close(TIMEOUT);
        processor.close(TIMEOUT);
        assertThrows(IllegalStateException.class, processor::start);
    }

    @Test
    public void skal_melde_selvventing_fra_callback_og_likevel_slette_fullfort_record() {
        KafkaConsumerRepository repository = mock(KafkaConsumerRepository.class);
        CountDownLatch callback = new CountDownLatch(1);
        when(repository.getTopicPartitions(anyList())).thenReturn(List.of(PARTITION));
        when(repository.getRecords("topic", 0, 10)).thenReturn(List.of(storedRecord()));
        KafkaConsumerRecordProcessor[] holder = new KafkaConsumerRecordProcessor[1];
        try (KafkaConsumerRecordProcessor processor = processor(
                repository,
                consumerRecord -> {
                    assertThrows(IllegalStateException.class, () -> holder[0].close(TIMEOUT));
                    callback.countDown();
                    return ConsumeStatus.OK;
                },
                null)) {
            holder[0] = processor;
            processor.start();
            vent(callback);
            processor.close(TIMEOUT);
            verify(repository).deleteRecords(List.of(1L));
        }
    }

    private static KafkaConsumerRecordProcessor processor(
            KafkaConsumerRepository repository, TopicConsumer<byte[], byte[]> consumer, SimpleMeterRegistry registry) {
        return new KafkaConsumerRecordProcessor(
                configuration -> Optional.of(() -> {}),
                repository,
                Map.of("topic", consumer),
                new KafkaConsumerRecordProcessorConfig(
                        Duration.ofDays(1), Duration.ofDays(1), 10, storedRecord -> Duration.ZERO),
                registry);
    }

    private static StoredConsumerRecord storedRecord() {
        return new StoredConsumerRecord(1, "topic", 0, 0, new byte[] {1}, new byte[] {2}, "[]", 0, null, 0);
    }

    private static StoredConsumerRecord storedRecord(long id, byte[] key) {
        return new StoredConsumerRecord(
                id, "topic", 0, id, key, new byte[] {2}, "[]", 0, null, System.currentTimeMillis());
    }
}
