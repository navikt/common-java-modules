package no.nav.common.kafka.consumer;

import static no.nav.common.kafka.util.WorkerLifecycleTest.avbrytForsteArbeidsoppgave;
import static no.nav.common.kafka.util.WorkerLifecycleTest.ryddExecutor;
import static no.nav.common.kafka.util.WorkerLifecycleTest.vent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import no.nav.common.kafka.consumer.util.FeatureToggledKafkaConsumerClient;
import no.nav.common.kafka.util.WorkerLifecycle;
import org.apache.kafka.clients.consumer.CloseOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class KafkaConsumerClientLifecycleTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final TopicPartition PARTITION = new TopicPartition("topic", 0);

    @Test
    public void skal_returnere_fra_vanlig_stopp_etter_tidsavbrudd_mens_lukkingen_fortsetter() throws Exception {
        KafkaConsumer<String, String> consumer = consumer();
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        doAnswer(invocation -> {
                    closing.countDown();
                    assertTrue(release.await(30, TimeUnit.SECONDS));
                    assertFalse(Thread.currentThread().isInterrupted());
                    closed.countDown();
                    return null;
                })
                .when(consumer)
                .close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        ExecutorService callers = Executors.newSingleThreadExecutor();
        Throwable failure = null;
        try {
            client.start();
            Future<?> stopping = callers.submit(() -> client.stop());
            vent(closing);
            stopping.get(12, TimeUnit.SECONDS);
            assertEquals(1, closed.getCount());
            assertFalse(client.isRunning());
            assertThrows(IllegalStateException.class, () -> client.stop(Duration.ZERO));
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(TIMEOUT);
            assertEquals(0, closed.getCount());
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {
                release.countDown();
                client.close(TIMEOUT);
            });
        }
    }

    @Test
    public void skal_vente_pa_alle_partisjonsarbeidere_for_siste_commit_og_lukking() {
        KafkaConsumer<String, String> consumer = consumer();
        TopicPartition secondPartition = new TopicPartition("topic", 1);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(
                                PARTITION, List.of(new ConsumerRecord<>("topic", 0, 4, "key", "value")),
                                secondPartition, List.of(new ConsumerRecord<>("topic", 1, 7, "key", "value"))),
                        Map.of()));
        CountDownLatch consuming = new CountDownLatch(2);
        CountDownLatch firstRelease = new CountDownLatch(1);
        CountDownLatch secondRelease = new CountDownLatch(1);
        CountDownLatch firstCompleted = new CountDownLatch(1);
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
                    committed.set(Map.copyOf(invocation.getArgument(0)));
                    return null;
                })
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        KafkaConsumerClientImpl<String, String> client = client(
                consumerRecord -> {
                    consuming.countDown();
                    vent(consumerRecord.partition() == 0 ? firstRelease : secondRelease);
                    assertFalse(Thread.currentThread().isInterrupted());
                    if (consumerRecord.partition() == 0) {
                        firstCompleted.countDown();
                    }
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        try {
            client.start();
            vent(consuming);
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            firstRelease.countDown();
            vent(firstCompleted);
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            verify(consumer, never()).commitSync(anyMap(), any(Duration.class));
            verify(consumer, never()).close(any(CloseOptions.class));
            secondRelease.countDown();
            client.close(TIMEOUT);
            assertEquals(
                    Map.of(PARTITION, new OffsetAndMetadata(5), secondPartition, new OffsetAndMetadata(8)),
                    committed.get());
            InOrder order = Mockito.inOrder(consumer);
            order.verify(consumer).commitSync(anyMap(), any(Duration.class));
            order.verify(consumer).close(any(CloseOptions.class));
        } finally {
            firstRelease.countDown();
            secondRelease.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_vente_pa_partisjonsarbeid_etter_avbrutt_batchventing_for_commit_og_lukking() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return new ConsumerRecords<>(
                    Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of());
        });
        CountDownLatch consuming = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interruptedDuringClose = new AtomicBoolean();
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
                    committed.set(Map.copyOf(invocation.getArgument(0)));
                    return null;
                })
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        doAnswer(invocation -> {
                    interruptedDuringClose.set(Thread.currentThread().isInterrupted());
                    return null;
                })
                .when(consumer)
                .close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(
                consumerRecord -> {
                    consuming.countDown();
                    vent(release);
                    assertFalse(Thread.currentThread().isInterrupted());
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        try {
            client.start();
            vent(consuming);
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            verify(consumer, never()).commitSync(anyMap(), any(Duration.class));
            verify(consumer, never()).close(any(CloseOptions.class));
            release.countDown();
            client.close(TIMEOUT);
            assertEquals(Map.of(PARTITION, new OffsetAndMetadata(1)), committed.get());
            assertFalse(interruptedDuringClose.get());
            InOrder order = Mockito.inOrder(consumer);
            order.verify(consumer).commitSync(anyMap(), any(Duration.class));
            order.verify(consumer).close(any(CloseOptions.class));
        } finally {
            release.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_stoppe_ved_avbrutt_initialisering_og_gjenopprette_interrupt_etter_lukking() throws Exception {
        KafkaConsumer<String, String> consumer = consumer();
        AtomicBoolean interruptedDuringClose = new AtomicBoolean();
        doAnswer(invocation -> {
                    interruptedDuringClose.set(Thread.currentThread().isInterrupted());
                    return null;
                })
                .when(consumer)
                .close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean interruptedAfterWork = new AtomicBoolean();
        try {
            avbrytForsteArbeidsoppgave(client, completed, interruptedAfterWork);
            client.start();
            vent(completed);
            assertFalse(client.isRunning());
            assertTrue(interruptedAfterWork.get());
            assertFalse(interruptedDuringClose.get());
            verify(consumer, never()).poll(any(Duration.class));
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_bevare_synkron_oppstartsfeil_og_lukke_delvis_opprettet_consumer() {
        KafkaConsumer<String, String> consumer = consumer();
        doThrow(new IllegalArgumentException("Simulert oppstartsfeil"))
                .when(consumer)
                .subscribe(anyList(), any(ConsumerRebalanceListener.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        try {
            assertThrows(IllegalArgumentException.class, client::start);
            client.close(TIMEOUT);
            verify(consumer).close(any(CloseOptions.class));
            assertFalse(client.isRunning());
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_bevare_error_fra_oppstart_og_lukke_delvis_opprettet_consumer() {
        KafkaConsumer<String, String> consumer = consumer();
        AssertionError failure = new AssertionError("Simulert oppstartsfeil");
        doThrow(failure).when(consumer).subscribe(anyList(), any(ConsumerRebalanceListener.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        try {
            assertEquals(failure, assertThrows(AssertionError.class, client::start));
            assertFalse(client.isRunning());
            client.close(TIMEOUT);
            verify(consumer, never()).poll(any(Duration.class));
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_avvente_samtidig_oppretting_uten_a_pollere_etter_lukking() throws Exception {
        KafkaConsumer<String, String> consumer = consumer();
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService callers = Executors.newSingleThreadExecutor();
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> {
            creating.countDown();
            vent(release);
            return consumer;
        });
        Throwable failure = null;
        try {
            Future<?> start = callers.submit(client::start);
            vent(creating);
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            start.get(3, TimeUnit.SECONDS);
            client.close(TIMEOUT);
            verify(consumer, Mockito.never()).poll(any(Duration.class));
            verify(consumer).close(any(CloseOptions.class));
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {
                release.countDown();
                client.close(TIMEOUT);
            });
        }
    }

    @Test
    public void skal_avvise_selvventing_fra_callback_gjennom_toggle_wrapper() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of()));
        CountDownLatch callback = new CountDownLatch(1);
        FeatureToggledKafkaConsumerClient[] holder = new FeatureToggledKafkaConsumerClient[1];
        KafkaConsumerClientImpl<String, String> underlying = client(
                consumerRecord -> {
                    assertThrows(IllegalStateException.class, () -> holder[0].close(TIMEOUT));
                    callback.countDown();
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        holder[0] = new FeatureToggledKafkaConsumerClient(underlying, () -> false, Duration.ofDays(1));
        try {
            holder[0].start();
            vent(callback);
            holder[0].close(TIMEOUT);
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            holder[0].close(TIMEOUT);
        }
    }

    @Test
    public void skal_tillate_aktiv_start_men_avvise_callback_omstart_under_stopp_av_nestede_wrappere() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of()));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean activeStartReturned = new AtomicBoolean();
        AtomicReference<IllegalStateException> rejected = new AtomicReference<>();
        doAnswer(invocation -> {
                    wakeup.countDown();
                    return null;
                })
                .when(consumer)
                .wakeup();
        FeatureToggledKafkaConsumerClient[] holder = new FeatureToggledKafkaConsumerClient[1];
        KafkaConsumerClientImpl<String, String> underlying = client(
                consumerRecord -> {
                    holder[0].start();
                    activeStartReturned.set(true);
                    entered.countDown();
                    vent(wakeup);
                    rejected.set(assertThrows(IllegalStateException.class, holder[0]::start));
                    completed.countDown();
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        FeatureToggledKafkaConsumerClient inner =
                new FeatureToggledKafkaConsumerClient(underlying, () -> false, Duration.ofDays(1));
        holder[0] = new FeatureToggledKafkaConsumerClient(inner, () -> false, Duration.ofDays(1));
        try {
            holder[0].start();
            vent(entered);
            holder[0].stop();
            vent(completed);
            assertTrue(activeStartReturned.get());
            assertEquals(
                    "A Kafka worker cannot wait for feature toggle termination",
                    rejected.get().getMessage());
            holder[0].close(TIMEOUT);
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            wakeup.countDown();
            holder[0].close(TIMEOUT);
        }
    }

    @Test
    public void skal_opprette_ny_consumer_etter_uventet_wakeup() throws Exception {
        KafkaConsumer<String, String> first = consumer();
        KafkaConsumer<String, String> second = consumer();
        when(first.poll(any(Duration.class))).thenThrow(new WakeupException());
        CountDownLatch recovered = new CountDownLatch(1);
        when(second.poll(any(Duration.class))).thenAnswer(invocation -> {
            recovered.countDown();
            return ConsumerRecords.empty();
        });
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> {
            if (created.incrementAndGet() == 1) {
                return first;
            }
            verify(first).close(any(CloseOptions.class));
            return second;
        });
        try {
            client.start();
            assertTrue(recovered.await(7, TimeUnit.SECONDS));
            client.close(TIMEOUT);
            verify(second).close(any(CloseOptions.class));
            assertEquals(2, created.get());
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_lukke_ny_consumer_uten_poll_nar_lukking_starter_under_reinitialisering() throws Exception {
        KafkaConsumer<String, String> first = consumer();
        KafkaConsumer<String, String> second = consumer();
        when(first.poll(any(Duration.class))).thenThrow(new WakeupException());
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> {
            if (created.incrementAndGet() == 1) {
                return first;
            }
            verify(first).close(any(CloseOptions.class));
            creating.countDown();
            vent(release);
            return second;
        });
        try {
            client.start();
            assertTrue(creating.await(7, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(TIMEOUT);
            verify(second, never()).poll(any(Duration.class));
            verify(second).close(any(CloseOptions.class));
            assertEquals(2, created.get());
            assertFalse(client.isRunning());
        } finally {
            release.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_melde_consumer_lukkefeil_ved_eksplisitt_lukking() {
        KafkaConsumer<String, String> consumer = consumer();
        doThrow(new IllegalStateException("Simulert lukkefeil")).when(consumer).close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        client.start();
        assertThrows(IllegalStateException.class, () -> client.close(TIMEOUT));
        assertThrows(IllegalStateException.class, () -> client.close(TIMEOUT));
        verify(consumer).close(any(CloseOptions.class));
    }

    @Test
    public void skal_bevare_error_fra_consumer_lukking_ved_stopp_og_gjentatt_close() {
        KafkaConsumer<String, String> consumer = consumer();
        AssertionError failure = new AssertionError("Simulert Error fra consumer-lukking");
        doThrow(failure).when(consumer).close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        client.start();
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> client.stop(TIMEOUT))
                        .getCause());
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> client.close(TIMEOUT))
                        .getCause()
                        .getCause());
        assertEquals(
                failure,
                assertThrows(IllegalStateException.class, () -> client.close(TIMEOUT))
                        .getCause()
                        .getCause());
        verify(consumer).close(any(CloseOptions.class));
    }

    @Test
    public void skal_melde_consumer_lukkefeil_fra_bade_bekreftet_og_vanlig_stopp() {
        KafkaConsumer<String, String> consumer = consumer();
        IllegalStateException failure = new IllegalStateException("Simulert lukkefeil");
        doThrow(failure).when(consumer).close(any(CloseOptions.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        client.start();
        Throwable closeFailure = assertThrows(IllegalStateException.class, () -> client.stop(TIMEOUT))
                .getCause();
        assertEquals(failure, closeFailure.getCause());
        assertEquals(
                closeFailure,
                assertThrows(IllegalStateException.class, client::stop).getCause());
        assertEquals(
                closeFailure,
                assertThrows(IllegalStateException.class, () -> client.close(TIMEOUT))
                        .getCause());
        verify(consumer).close(any(CloseOptions.class));
    }

    @Test
    public void skal_bevare_stoppet_kjorings_lukkefeil_ved_samtidig_omstart() throws Exception {
        KafkaConsumer<String, String> first = consumer();
        KafkaConsumer<String, String> second = consumer();
        IllegalStateException closeFailure = new IllegalStateException("Simulert lukkefeil");
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
                    closing.countDown();
                    vent(release);
                    throw closeFailure;
                })
                .when(first)
                .close(any(CloseOptions.class));
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client =
                client(consumerRecord -> ConsumeStatus.OK, () -> created.incrementAndGet() == 1 ? first : second);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        Throwable failure = null;
        try {
            client.start();
            Future<IllegalStateException> stopping =
                    callers.submit(() -> assertThrows(IllegalStateException.class, () -> client.stop(TIMEOUT)));
            vent(closing);
            Future<?> restarting = callers.submit(client::start);
            release.countDown();
            restarting.get(3, TimeUnit.SECONDS);
            assertEquals(
                    closeFailure, stopping.get(3, TimeUnit.SECONDS).getCause().getCause());
            client.close(TIMEOUT);
            verify(first).close(any(CloseOptions.class));
            verify(second).close(any(CloseOptions.class));
            assertEquals(2, created.get());
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            ryddExecutor(callers, failure, () -> {
                release.countDown();
                client.close(TIMEOUT);
            });
        }
    }

    @Test
    public void skal_vekke_langvarig_poll_og_lukke_consumer_fra_shutdown_hook() throws Exception {
        KafkaConsumer<String, String> consumer = consumer();
        CountDownLatch polling = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        Duration pollDuration = Duration.ofSeconds(30);
        when(consumer.poll(pollDuration)).thenAnswer(invocation -> {
            polling.countDown();
            assertTrue(wakeup.await(30, TimeUnit.SECONDS));
            throw new WakeupException();
        });
        doAnswer(invocation -> {
                    wakeup.countDown();
                    return null;
                })
                .when(consumer)
                .wakeup();
        Properties properties = new Properties();
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(
                new KafkaConsumerClientConfig<>(
                        properties, Map.of("topic", consumerRecord -> ConsumeStatus.OK), pollDuration.toMillis()),
                () -> consumer);
        try {
            client.start();
            vent(polling);
            Field lifecycleField = KafkaConsumerClientImpl.class.getDeclaredField("lifecycle");
            lifecycleField.setAccessible(true);
            WorkerLifecycle lifecycle = (WorkerLifecycle) lifecycleField.get(client);
            Field hookField = WorkerLifecycle.class.getDeclaredField("shutdownHook");
            hookField.setAccessible(true);
            Thread shutdownHook = (Thread) hookField.get(lifecycle);
            AtomicReference<Throwable> shutdownFailure = new AtomicReference<>();
            shutdownHook.setUncaughtExceptionHandler((thread, failure) -> shutdownFailure.set(failure));
            shutdownHook.start();
            shutdownHook.join(TimeUnit.SECONDS.toMillis(3));
            assertFalse("Shutdown-hooken ble ikke ferdig", shutdownHook.isAlive());
            assertNull(shutdownFailure.get());
            verify(consumer).wakeup();
            verify(consumer).close(any(CloseOptions.class));
            assertFalse(client.isRunning());
        } finally {
            wakeup.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_lukke_for_start_uten_a_opprette_consumer() {
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> {
            created.incrementAndGet();
            return consumer();
        });
        client.close(TIMEOUT);
        client.close(TIMEOUT);
        assertEquals(0, created.get());
        assertThrows(IllegalStateException.class, client::start);
    }

    @Test
    public void skal_vente_pa_callback_bevare_offsets_og_ikke_kjore_neste_record_etter_stopp() {
        KafkaConsumer<String, String> consumer = consumer();
        ConsumerRecord<String, String> first = new ConsumerRecord<>("topic", 0, 10, "key", "value");
        ConsumerRecord<String, String> second = new ConsumerRecord<>("topic", 0, 11, "key", "value");
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(Map.of(PARTITION, List.of(first, second)), Map.of()));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger consumed = new AtomicInteger();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean pendingWakeup = new AtomicBoolean();
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
                    pendingWakeup.set(true);
                    return null;
                })
                .when(consumer)
                .wakeup();
        doAnswer(invocation -> {
                    if (pendingWakeup.getAndSet(false)) {
                        throw new WakeupException();
                    }
                    committed.set(Map.copyOf(invocation.getArgument(0)));
                    return null;
                })
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        KafkaConsumerClientImpl<String, String> client = client(
                consumerRecord -> {
                    consumed.incrementAndGet();
                    entered.countDown();
                    vent(release);
                    interrupted.set(Thread.currentThread().isInterrupted());
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        try {
            client.start();
            client.start();
            vent(entered);
            assertThrows(IllegalStateException.class, () -> client.stop(Duration.ZERO));
            assertFalse(client.isRunning());
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(TIMEOUT);
            assertEquals(1, consumed.get());
            assertFalse(interrupted.get());
            InOrder order = Mockito.inOrder(consumer);
            order.verify(consumer, times(2)).commitSync(anyMap(), any(Duration.class));
            order.verify(consumer).close(any(CloseOptions.class));
            assertEquals(Map.of(PARTITION, new OffsetAndMetadata(11)), committed.get());
        } finally {
            release.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_prove_siste_commit_igjen_nar_pending_wakeup_treffer_oppryddingen() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of()));
        CountDownLatch committing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean pendingWakeup = new AtomicBoolean();
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
                    pendingWakeup.set(true);
                    return null;
                })
                .when(consumer)
                .wakeup();
        doAnswer(invocation -> {
                    if (attempts.incrementAndGet() == 1) {
                        committing.countDown();
                        vent(release);
                        throw new IllegalStateException("Simulert commitfeil");
                    }
                    if (pendingWakeup.getAndSet(false)) {
                        throw new WakeupException();
                    }
                    committed.set(Map.copyOf(invocation.getArgument(0)));
                    return null;
                })
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        KafkaConsumerClientImpl<String, String> client = client(consumerRecord -> ConsumeStatus.OK, () -> consumer);
        try {
            client.start();
            vent(committing);
            assertThrows(IllegalStateException.class, () -> client.stop(Duration.ZERO));
            assertThrows(IllegalStateException.class, () -> client.close(Duration.ZERO));
            release.countDown();
            client.close(TIMEOUT);
            assertEquals(3, attempts.get());
            assertEquals(Map.of(PARTITION, new OffsetAndMetadata(1)), committed.get());
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            release.countDown();
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_lukke_consumer_selv_om_siste_commit_feiler() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of()));
        doThrow(new IllegalStateException("Simulert commitfeil"))
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        CountDownLatch consumed = new CountDownLatch(1);
        KafkaConsumerClientImpl<String, String> client = client(
                consumerRecord -> {
                    consumed.countDown();
                    return ConsumeStatus.OK;
                },
                () -> consumer);
        try {
            client.start();
            vent(consumed);
            client.close(TIMEOUT);
            verify(consumer).close(any(CloseOptions.class));
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_opprette_ny_consumer_ved_omstart_etter_stopp() {
        AtomicInteger created = new AtomicInteger();
        CountDownLatch firstPoll = new CountDownLatch(1);
        CountDownLatch secondPoll = new CountDownLatch(1);
        KafkaConsumer<String, String> first = consumer();
        KafkaConsumer<String, String> second = consumer();
        when(first.poll(any(Duration.class))).thenAnswer(invocation -> {
            firstPoll.countDown();
            return ConsumerRecords.empty();
        });
        when(second.poll(any(Duration.class))).thenAnswer(invocation -> {
            secondPoll.countDown();
            return ConsumerRecords.empty();
        });
        KafkaConsumerClientImpl<String, String> client =
                client(consumerRecord -> ConsumeStatus.OK, () -> created.incrementAndGet() == 1 ? first : second);
        try {
            client.start();
            vent(firstPoll);
            client.stop();
            verify(first).close(any(CloseOptions.class));
            client.start();
            vent(secondPoll);
            client.close(TIMEOUT);
            verify(second).close(any(CloseOptions.class));
            assertEquals(2, created.get());
        } finally {
            client.close(TIMEOUT);
        }
    }

    @Test
    public void skal_avvise_selvventing_fra_callback_uten_a_miste_offset() {
        KafkaConsumer<String, String> consumer = consumer();
        when(consumer.poll(any(Duration.class)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(PARTITION, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))), Map.of()));
        CountDownLatch callback = new CountDownLatch(1);
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
                    committed.set(Map.copyOf(invocation.getArgument(0)));
                    return null;
                })
                .when(consumer)
                .commitSync(anyMap(), any(Duration.class));
        AtomicReference<KafkaConsumerClientImpl<String, String>> holder = new AtomicReference<>();
        holder.set(client(
                consumerRecord -> {
                    assertThrows(IllegalStateException.class, holder.get()::stop);
                    assertThrows(IllegalStateException.class, () -> holder.get().close(TIMEOUT));
                    callback.countDown();
                    return ConsumeStatus.OK;
                },
                () -> consumer));
        try {
            holder.get().start();
            vent(callback);
            holder.get().close(TIMEOUT);
            verify(consumer, times(1)).commitSync(anyMap(), any(Duration.class));
            assertEquals(Map.of(PARTITION, new OffsetAndMetadata(1)), committed.get());
        } finally {
            holder.get().close(TIMEOUT);
        }
    }

    private static KafkaConsumerClientImpl<String, String> client(
            TopicConsumer<String, String> topicConsumer, Supplier<KafkaConsumer<String, String>> factory) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumerClientImpl<>(
                new KafkaConsumerClientConfig<>(properties, Map.of("topic", topicConsumer)), factory);
    }

    @SuppressWarnings("unchecked")
    private static KafkaConsumer<String, String> consumer() {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        when(consumer.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());
        return consumer;
    }
}
