package no.nav.common.kafka.consumer;

import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class KafkaConsumerClientLifecycleTest {

    @Test
    public void constructor_and_builder_do_not_allocate_a_kafka_consumer_or_start_consumption() {
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class)) {
            KafkaConsumerClientImpl<String, String> direct = client(record -> ConsumeStatus.OK, 1000);
            Properties properties = new Properties();
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            KafkaConsumerClient built = KafkaConsumerClientBuilder.builder()
                    .withProperties(properties)
                    .withTopicConfig(new KafkaConsumerClientBuilder.TopicConfig<String, String>()
                            .withConsumerConfig("topic", new StringDeserializer(), new StringDeserializer(),
                                    (TopicConsumer<String, String>) record -> ConsumeStatus.OK))
                    .build();
            assertFalse(direct.isRunning());
            assertFalse(built.isRunning());
            assertTrue(consumers.constructed().isEmpty());
            direct.stop();
            built.stop();
            assertTrue(consumers.constructed().isEmpty());
        }
    }

    @Test
    public void start_and_stop_are_idempotent_and_restart_uses_a_new_consumer_and_executor() throws Exception {
        try (MockedConstruction<KafkaConsumer> consumers = blockingConsumers()) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            client.stop();
            client.start();
            KafkaConsumerExecution<?, ?> firstRun = run(client);
            assertEquals(KafkaConsumerExecution.Phase.CONSUMING, phase(firstRun));
            client.start();
            assertEquals(1, consumers.constructed().size());

            client.stop();
            client.stop();
            assertTrue(firstRun.isFullyStopped());
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            assertFalse(client.isRunning());

            client.start();
            assertEquals(2, consumers.constructed().size());
            KafkaConsumerExecution<?, ?> secondRun = run(client);
            assertNotSame(firstRun, secondRun);
            client.stop();
            assertTrue(secondRun.isFullyStopped());
        }
    }

    @Test
    public void failed_subscribe_closes_consumer_and_preserves_cleanup_failure() {
        RuntimeException failure = new IllegalArgumentException("subscribe");
        RuntimeException cleanupFailure = new IllegalStateException("close");
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            doThrow(failure).when(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
            doThrow(cleanupFailure).doThrow(cleanupFailure).doNothing().when(consumer).close(any(Duration.class));
        })) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            assertSame(failure, assertThrows(IllegalArgumentException.class, client::start));
            assertArrayEquals(new Throwable[]{cleanupFailure}, failure.getSuppressed());
            assertFalse(client.isRunning());
            assertThrows(IllegalStateException.class, client::start);
            assertEquals(1, consumers.constructed().size());
            assertSame(cleanupFailure, assertThrows(IllegalStateException.class, client::stop));
            assertThrows(IllegalStateException.class, client::start);
            client.stop();
            verify(consumers.constructed().get(0), times(3)).close(any(Duration.class));
        }
    }

    @Test
    public void same_start_and_close_failure_is_not_added_as_its_own_suppressed_exception() {
        RuntimeException failure = new IllegalArgumentException("subscribe and close");
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            doThrow(failure).when(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
            doThrow(failure).doNothing().when(consumer).close(any(Duration.class));
        })) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            assertSame(failure, assertThrows(IllegalArgumentException.class, client::start));
            assertEquals(0, failure.getSuppressed().length);
            client.stop();
            verify(consumers.constructed().get(0), times(2)).close(any(Duration.class));
        }
    }

    @Test
    public void start_can_be_retried_after_subscribe_failure() {
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            if (context.getCount() == 1) {
                doThrow(new IllegalArgumentException("subscribe")).when(consumer)
                        .subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
            } else {
                blockPoll(consumer);
            }
        })) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            assertThrows(IllegalArgumentException.class, client::start);
            client.start();
            client.stop();
            assertEquals(2, consumers.constructed().size());
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            verify(consumers.constructed().get(1)).close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void failed_submission_shuts_down_executor_and_closes_consumer() throws Exception {
        ExecutorService rejectedExecutor = mock(ExecutorService.class);
        RejectedExecutionException failure = new RejectedExecutionException("submit");
        doThrow(failure).when(rejectedExecutor).execute(any(Runnable.class));
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class);
             MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(rejectedExecutor);
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            assertSame(failure, assertThrows(RejectedExecutionException.class, client::start));
            verify(rejectedExecutor).shutdown();
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            assertFalse(client.isRunning());
            when(rejectedExecutor.isTerminated()).thenReturn(true);
            when(rejectedExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
            client.stop();
        }
    }

    @Test
    public void construction_failure_is_synchronous_and_starting_is_not_running() throws Exception {
        RuntimeException failure = new IllegalArgumentException("construct");
        AtomicReference<KafkaConsumerExecution<String, String>> run = new AtomicReference<>();
        run.set(new KafkaConsumerExecution<>(config(record -> ConsumeStatus.OK), () -> {
            try {
                assertEquals(KafkaConsumerExecution.Phase.STARTING, phase(run.get()));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            assertFalse(run.get().isRunning());
            assertFalse(run.get().isFullyStopped());
            throw failure;
        }));
        assertEquals(KafkaConsumerExecution.Phase.NEW, phase(run.get()));
        assertSame(failure, assertThrows(IllegalArgumentException.class, run.get()::initialize));
        assertFalse(run.get().isRunning());
        run.get().observeStartupTermination();
        assertTrue(run.get().isFullyStopped());
    }

    @Test
    public void unexpected_execution_exception_is_surfaced_even_without_a_recorded_worker_failure() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        AtomicReference<Runnable> task = new AtomicReference<>();
        doAnswer(invocation -> { task.set(invocation.getArgument(0)); return null; })
                .when(executor).execute(any(Runnable.class));
        RuntimeException failure = new IllegalStateException("task failed before returning result");
        AtomicInteger shutdowns = new AtomicInteger();
        doAnswer(invocation -> {
            if (shutdowns.incrementAndGet() == 1) {
                task.get().run();
                return null;
            }
            throw failure;
        }).when(executor).shutdown();
        when(executor.isTerminated()).thenReturn(true);
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(executor);
            KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000, () -> consumer);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            assertSame(failure, assertThrows(IllegalStateException.class, client::stop));
            assertTrue(run.isFullyStopped());
            verify(consumer, never()).poll(any(Duration.class));
            verify(consumer).close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void fatal_polling_error_is_cleaned_up_without_recovery_and_surfaced_on_stop() throws Exception {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        AssertionError failure = new AssertionError("fatal poll");
        when(consumer.poll(any(Duration.class))).thenThrow(failure);
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000,
                () -> { created.incrementAndGet(); return consumer; });
        client.start();
        KafkaConsumerExecution<?, ?> run = run(client);
        await().atMost(Duration.ofSeconds(2)).until(() -> pollingExecutor(client).isTerminated());
        assertEquals(KafkaConsumerExecution.Phase.SHUTTING_DOWN, phase(run));
        assertFalse(run.isRunning());
        assertFalse(run.isFullyStopped());
        assertSame(failure, assertThrows(AssertionError.class, client::stop));
        assertTrue(run.isFullyStopped());
        verify(consumer).close(Duration.ofSeconds(3));
        assertEquals(1, created.get());
        client.stop();
    }

    @Test
    public void completed_worker_is_not_stopped_until_executor_termination_is_confirmed() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        AssertionError failure = new AssertionError("fatal poll");
        when(consumer.poll(any(Duration.class))).thenThrow(failure);
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
                .when(executor).execute(any(Runnable.class));
        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(executor);
            KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1, () -> consumer);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            IllegalStateException timeout = assertThrows(IllegalStateException.class,
                    client::stop);
            assertTrue(timeout.getCause() instanceof TimeoutException);
            assertFalse(run.isFullyStopped());
            assertEquals(KafkaConsumerExecution.Phase.SHUTTING_DOWN, phase(run));
            when(executor.isTerminated()).thenReturn(true);
            when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
            assertSame(failure, assertThrows(AssertionError.class, client::stop));
            assertEquals(KafkaConsumerExecution.Phase.STOPPED, phase(run));
        }
    }

    @Test
    public void exceptional_task_completion_preserves_original_failure_and_unclosed_resource() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        AssertionError original = new AssertionError("fatal poll");
        RuntimeException closeFailure = new IllegalStateException("close");
        RuntimeException taskFailure = new IllegalStateException("executor shutdown");
        when(consumer.poll(any(Duration.class))).thenThrow(original);
        doThrow(closeFailure).doNothing().when(consumer).close(any(Duration.class));
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
                .when(executor).execute(any(Runnable.class));
        doThrow(taskFailure).doNothing().when(executor).shutdown();
        when(executor.isTerminated()).thenReturn(true);
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(executor);
            KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000, () -> consumer);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            verify(consumer).close(Duration.ofSeconds(3));
            assertFalse(run.isFullyStopped());
            assertSame(original, assertThrows(AssertionError.class, client::stop));
            assertArrayEquals(new Throwable[]{closeFailure, taskFailure}, original.getSuppressed());
            verify(consumer, times(2)).close(any(Duration.class));
            assertTrue(run.isFullyStopped());
        }
    }

    @Test
    public void fatal_close_error_is_not_retried_by_the_worker() throws Exception {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        CountDownLatch polled = new CountDownLatch(1);
        CountDownLatch wake = new CountDownLatch(1);
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            polled.countDown();
            awaitLatch(wake);
            throw new WakeupException();
        });
        doAnswer(invocation -> { wake.countDown(); return null; }).when(consumer).wakeup();
        AssertionError failure = new AssertionError("fatal close");
        doThrow(failure).doNothing().when(consumer).close(any(Duration.class));
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000,
                () -> consumer);
        client.start();
        KafkaConsumerExecution<?, ?> run = run(client);
        try {
            assertTrue(polled.await(2, TimeUnit.SECONDS));
            assertSame(failure, assertThrows(AssertionError.class, client::stop));
            // One worker close, then one caller retry after executor termination.
            verify(consumer, times(2)).close(any(Duration.class));
            assertTrue(run.isFullyStopped());
        } finally {
            wake.countDown();
            client.stop();
        }
    }

    @Test
    public void failed_worker_and_caller_close_keep_resource_until_a_later_stop_retry() throws Exception {
        RuntimeException failure = new IllegalStateException("close");
        try (MockedConstruction<KafkaConsumer> consumers = blockingConsumers(consumer ->
                doThrow(failure).doThrow(failure).doThrow(failure).doNothing()
                        .when(consumer).close(any(Duration.class)))) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            assertSame(failure, assertThrows(IllegalStateException.class, client::stop));
            assertEquals(KafkaConsumerExecution.Phase.CLEANUP_REQUIRED, phase(run));
            assertFalse(run.isRunning());
            assertFalse(run.isFullyStopped());
            assertTrue(pollingExecutor(client).isTerminated());
            assertThrows(IllegalStateException.class, client::start);
            client.stop();
            assertEquals(KafkaConsumerExecution.Phase.STOPPED, phase(run));
            verify(consumers.constructed().get(0), times(4)).close(any(Duration.class));
        }
    }

    @Test
    public void concurrent_stops_both_wait_until_consumer_close_completes() throws Exception {
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try (MockedConstruction<KafkaConsumer> consumers = blockingConsumers(consumer ->
                doAnswer(invocation -> {
                    closing.countDown();
                    awaitLatch(release);
                    return null;
                }).when(consumer).close(any(Duration.class)))) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 5000);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            Future<?> first = callers.submit(client::stop);
            try {
                assertTrue(closing.await(2, TimeUnit.SECONDS));
                CountDownLatch secondEntered = new CountDownLatch(1);
                Future<?> second = callers.submit(() -> {
                    secondEntered.countDown();
                    client.stop();
                });
                assertTrue(secondEntered.await(2, TimeUnit.SECONDS));
                assertFalse(first.isDone());
                assertFalse(second.isDone());
                release.countDown();
                first.get(2, TimeUnit.SECONDS);
                second.get(2, TimeUnit.SECONDS);
                assertTrue(run.isFullyStopped());
                verify(consumers.constructed().get(0)).wakeup();
                verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            } finally {
                release.countDown();
                client.stop();
            }
        } finally {
            callers.shutdown();
            assertTrue(callers.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    public void timeout_keeps_old_generation_until_handler_finishes_and_stop_can_be_retried() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committedOffsets = new AtomicReference<>();
        try (MockedConstruction<KafkaConsumer> consumers = recordConsumers(consumer ->
                doAnswer(invocation -> {
                    committedOffsets.set(new HashMap<>(invocation.getArgument(0)));
                    return null;
                }).when(consumer).commitSync(anyMap(), any(Duration.class)))) {
            KafkaConsumerClientImpl<String, String> client = client(record -> {
                entered.countDown();
                awaitLatch(release);
                return ConsumeStatus.OK;
            }, 20);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            try {
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class, client::stop);
                assertThrows(IllegalStateException.class, client::stop);
                assertThrows(IllegalStateException.class, client::start);
                assertEquals(1, consumers.constructed().size());
                verify(consumers.constructed().get(0), never()).close(any(Duration.class));
                assertFalse(client.isRunning());
            } finally {
                release.countDown();
                await().atMost(Duration.ofSeconds(2)).until(() -> pollingExecutor(client).isTerminated());
                client.stop();
            }
            assertEquals(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)), committedOffsets.get());
            verify(consumers.constructed().get(0)).commitSync(anyMap(), eq(Duration.ofSeconds(3)));
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
        }
    }

    @Test
    public void interrupted_stop_restores_interrupt_and_can_be_retried() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (MockedConstruction<KafkaConsumer> ignored = recordConsumers()) {
            KafkaConsumerClientImpl<String, String> client = client(record -> {
                entered.countDown();
                awaitLatch(release);
                return ConsumeStatus.OK;
            }, 1000);
            client.start();
            try {
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                Thread.currentThread().interrupt();
                IllegalStateException failure = assertThrows(IllegalStateException.class, client::stop);
                assertTrue(failure.getCause() instanceof InterruptedException);
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
                release.countDown();
                client.stop();
            }
        }
    }

    @Test
    public void unexpected_exit_closes_old_consumer_and_automatically_restarts_on_same_executor() throws Exception {
        KafkaConsumer<String, String> first = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> second = mock(KafkaConsumer.class);
        when(first.poll(any(Duration.class))).thenReturn(records()).thenThrow(new WakeupException());
        doThrow(new IllegalStateException("commit")).when(first).commitSync(anyMap(), any(Duration.class));
        CountDownLatch restarted = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        when(second.poll(any(Duration.class))).thenAnswer(invocation -> {
            restarted.countDown();
            awaitLatch(wakeup);
            throw new WakeupException();
        });
        doAnswer(invocation -> { wakeup.countDown(); return null; }).when(second).wakeup();
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000,
                () -> {
                    int generation = created.incrementAndGet();
                    assertTrue(generation <= 2);
                    return generation == 1 ? first : second;
                });
        client.start();
        KafkaConsumerExecution<?, ?> worker = run(client);
        try {
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            assertTrue(client.isRunning());
            assertEquals(KafkaConsumerExecution.Phase.CONSUMING, phase(worker));
            assertSame(worker, run(client));
            verify(first, times(2)).poll(any(Duration.class));
            verify(first, times(2)).commitSync(anyMap(), any(Duration.class));
            verify(first).close(Duration.ofSeconds(3));
            verify(second).subscribe(anyCollection(), same(worker));
        } finally {
            client.stop();
        }
        assertTrue(worker.isFullyStopped());
        verify(second).wakeup();
        verify(first, never()).wakeup();
        verify(second).close(Duration.ofSeconds(3));
        assertEquals(2, created.get());
    }

    @Test
    public void failed_background_subscribe_is_fully_closed_before_another_consumer_is_created() throws Exception {
        KafkaConsumer<String, String> first = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> failed = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> second = mock(KafkaConsumer.class);
        when(first.poll(any(Duration.class))).thenThrow(new WakeupException());
        RuntimeException startFailure = new IllegalArgumentException("subscribe");
        RuntimeException cleanupFailure = new IllegalStateException("close");
        doThrow(startFailure).when(failed).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
        AtomicInteger closeAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (closeAttempts.incrementAndGet() == 1) {
                throw cleanupFailure;
            }
            return null;
        }).when(failed).close(any(Duration.class));
        CountDownLatch restarted = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        when(second.poll(any(Duration.class))).thenAnswer(invocation -> {
            restarted.countDown();
            awaitLatch(wakeup);
            throw new WakeupException();
        });
        doAnswer(invocation -> { wakeup.countDown(); return null; }).when(second).wakeup();
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000,
                () -> {
                    int generation = created.incrementAndGet();
                    if (generation == 1) {
                        return first;
                    }
                    if (generation == 2) {
                        return failed;
                    }
                    assertEquals(3, generation);
                    assertEquals(2, closeAttempts.get());
                    return second;
                });
        client.start();
        KafkaConsumerExecution<?, ?> run = run(client);
        try {
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            assertTrue(client.isRunning());
            assertArrayEquals(new Throwable[]{cleanupFailure}, startFailure.getSuppressed());
            verify(failed, never()).poll(any(Duration.class));
            verify(failed, times(2)).close(Duration.ofSeconds(3));
        } finally {
            client.stop();
        }
        assertTrue(run.isFullyStopped());
        assertEquals(3, created.get());
    }

    @Test
    public void final_commit_failure_still_attempts_close_and_preserves_close_failure() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RuntimeException commitFailure = new IllegalStateException("commit");
        RuntimeException closeFailure = new IllegalStateException("close");
        try (MockedConstruction<KafkaConsumer> consumers = recordConsumers(consumer -> {
            doThrow(commitFailure).when(consumer).commitSync(anyMap(), any(Duration.class));
            doThrow(closeFailure).doNothing().when(consumer).close(any(Duration.class));
            doAnswer(invocation -> { release.countDown(); return null; }).when(consumer).wakeup();
        })) {
            KafkaConsumerClientImpl<String, String> client = client(record -> {
                entered.countDown();
                awaitLatch(release);
                return ConsumeStatus.OK;
            }, 1000);
            client.start();
            KafkaConsumerExecution<?, ?> run = run(client);
            KafkaConsumer consumer = consumers.constructed().get(0);
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                client.stop();
                verify(consumer, times(2)).commitSync(anyMap(), any(Duration.class));
                verify(consumer, times(2)).close(Duration.ofSeconds(3));
                assertArrayEquals(new Throwable[]{closeFailure}, commitFailure.getSuppressed());
                assertTrue(run.isFullyStopped());
            } finally {
                release.countDown();
                client.stop();
            }
        }
    }

    @Test
    public void stop_times_out_during_replacement_and_late_candidate_is_closed_without_polling() throws Exception {
        KafkaConsumer<String, String> first = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> second = mock(KafkaConsumer.class);
        when(first.poll(any(Duration.class))).thenThrow(new WakeupException());
        CountDownLatch replacing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 20,
                () -> {
                    if (created.incrementAndGet() == 1) {
                        return first;
                    }
                    replacing.countDown();
                    awaitLatch(release);
                    return second;
                });
        client.start();
        KafkaConsumerExecution<?, ?> run = run(client);
        try {
            assertTrue(replacing.await(2, TimeUnit.SECONDS));
            assertTrue(client.isRunning());
            assertEquals(KafkaConsumerExecution.Phase.RECOVERING, phase(run));
            client.start();
            assertEquals(2, created.get());
            assertThrows(IllegalStateException.class, client::stop);
            assertFalse(client.isRunning());
            assertThrows(IllegalStateException.class, client::start);
            assertThrows(IllegalStateException.class, client::stop);
        } finally {
            release.countDown();
            await().atMost(Duration.ofSeconds(2)).until(() -> pollingExecutor(client).isTerminated());
            client.stop();
        }
        assertEquals(2, created.get());
        verify(first).close(Duration.ofSeconds(3));
        verify(second).close(Duration.ofSeconds(3));
        verify(second, never()).poll(any(Duration.class));
        verify(second, never()).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
    }

    @Test
    public void an_old_run_handle_cannot_stop_a_new_generation() throws Exception {
        try (MockedConstruction<KafkaConsumer> consumers = blockingConsumers()) {
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            client.start();
            KafkaConsumerExecution<?, ?> oldRun = run(client);
            client.stop();
            assertTrue(oldRun.isFullyStopped());
            client.start();
            KafkaConsumerExecution<?, ?> newRun = run(client);
            try {
                oldRun.requestShutdown();
                assertNotSame(oldRun, newRun);
                assertTrue(client.isRunning());
                verify(consumers.constructed().get(1), never()).wakeup();
                verify(consumers.constructed().get(1), never()).close(any(Duration.class));
            } finally {
                client.stop();
            }
            assertTrue(newRun.isFullyStopped());
        }
    }

    @Test
    public void shutdown_wakes_a_published_replacement_and_prevents_its_first_poll() throws Exception {
        KafkaConsumer<String, String> first = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> replacement = mock(KafkaConsumer.class);
        when(first.poll(any(Duration.class))).thenThrow(new WakeupException());
        CountDownLatch subscribing = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        doAnswer(invocation -> {
            subscribing.countDown();
            awaitLatch(wakeup);
            return null;
        }).when(replacement).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
        doAnswer(invocation -> { wakeup.countDown(); return null; }).when(replacement).wakeup();
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK), 1000,
                () -> created.incrementAndGet() == 1 ? first : replacement);
        client.start();
        KafkaConsumerExecution<?, ?> run = run(client);
        try {
            assertTrue(subscribing.await(2, TimeUnit.SECONDS));
            client.stop();
            assertTrue(run.isFullyStopped());
            verify(replacement).wakeup();
            verify(replacement).close(Duration.ofSeconds(3));
            verify(replacement, never()).poll(any(Duration.class));
            verify(first, never()).wakeup();
            assertEquals(2, created.get());
        } finally {
            wakeup.countDown();
            client.stop();
        }
    }

    @Test
    public void failed_record_skips_remaining_partition_records_and_commits_only_successful_offset() throws Exception {
        TopicPartition partition = new TopicPartition("topic", 0);
        ConsumerRecords<String, String> batch = new ConsumerRecords<>(Map.of(partition, List.of(
                new ConsumerRecord<>("topic", 0, 0, "key", "first"),
                new ConsumerRecord<>("topic", 0, 1, "key", "failed"),
                new ConsumerRecord<>("topic", 0, 2, "key", "must not run"))));
        List<Long> consumedOffsets = new CopyOnWriteArrayList<>();
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committedOffsets = new AtomicReference<>();
        CountDownLatch nextPoll = new CountDownLatch(1);
        CountDownLatch wakeup = new CountDownLatch(1);
        AtomicInteger polls = new AtomicInteger();
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
                if (polls.incrementAndGet() == 1) {
                    return batch;
                }
                nextPoll.countDown();
                awaitLatch(wakeup);
                throw new WakeupException();
            });
            doAnswer(invocation -> {
                committedOffsets.set(new HashMap<>(invocation.getArgument(0)));
                return null;
            }).when(consumer).commitSync(anyMap(), any(Duration.class));
            doAnswer(invocation -> { wakeup.countDown(); return null; }).when(consumer).wakeup();
        })) {
            KafkaConsumerClientImpl<String, String> client = client(record -> {
                consumedOffsets.add(record.offset());
                return record.offset() == 1 ? ConsumeStatus.FAILED : ConsumeStatus.OK;
            }, 1000);
            client.start();
            try {
                assertTrue(nextPoll.await(2, TimeUnit.SECONDS));
                assertEquals(List.of(0L, 1L), consumedOffsets);
                assertEquals(Map.of(partition, new OffsetAndMetadata(1)), committedOffsets.get());
                KafkaConsumer consumer = consumers.constructed().get(0);
                verify(consumer).seek(partition, new OffsetAndMetadata(0));
                verify(consumer).seek(partition, new OffsetAndMetadata(1));
                verify(consumer).commitSync(anyMap(), eq(Duration.ofSeconds(3)));
            } finally {
                client.stop();
            }
        }

    }

    @Test
    public void shutdown_drains_in_flight_handler_skips_later_records_then_commits_before_close() throws Exception {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        TopicPartition partition = new TopicPartition("topic", 0);
        when(consumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Map.of(partition, List.of(
                new ConsumerRecord<>("topic", 0, 0, "key", "first"),
                new ConsumerRecord<>("topic", 0, 1, "key", "must not run")))));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch shutdownRequested = new CountDownLatch(1);
        doAnswer(invocation -> { shutdownRequested.countDown(); return null; }).when(consumer).wakeup();
        List<Long> handled = new CopyOnWriteArrayList<>();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> {
            handled.add(record.offset());
            entered.countDown();
            awaitLatch(release);
            assertFalse(Thread.currentThread().isInterrupted());
            return ConsumeStatus.OK;
        }), 2000, () -> consumer);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        client.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Future<?> stopping = caller.submit(client::stop);
            assertTrue(shutdownRequested.await(2, TimeUnit.SECONDS));
            verify(consumer, never()).commitSync(anyMap(), any(Duration.class));
            verify(consumer, never()).close(any(Duration.class));
            assertFalse(stopping.isDone());
            release.countDown();
            stopping.get(2, TimeUnit.SECONDS);
            assertEquals(List.of(0L), handled);
            var order = inOrder(consumer);
            order.verify(consumer).seek(partition, new OffsetAndMetadata(0));
            order.verify(consumer).seek(partition, new OffsetAndMetadata(1));
            order.verify(consumer).commitSync(Map.of(partition, new OffsetAndMetadata(1)), Duration.ofSeconds(3));
            order.verify(consumer).close(Duration.ofSeconds(3));
        } finally {
            release.countDown();
            client.stop();
            caller.shutdown();
            assertTrue(caller.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    public void fatal_handler_exit_retains_all_partial_successes_through_drain_and_does_not_recover() throws Exception {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        TopicPartition first = new TopicPartition("topic", 0);
        TopicPartition second = new TopicPartition("topic", 1);
        when(consumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Map.of(
                first, List.of(new ConsumerRecord<>("topic", 0, 0, "key", "ok"),
                        new ConsumerRecord<>("topic", 0, 1, "key", "fatal"),
                        new ConsumerRecord<>("topic", 0, 2, "key", "must not run")),
                second, List.of(new ConsumerRecord<>("topic", 1, 5, "key", "in flight")))));
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch handlerFinished = new CountDownLatch(1);
        CountDownLatch failed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AssertionError fatal = new AssertionError("fatal handler");
        AtomicInteger created = new AtomicInteger();
        List<Long> handledFirstPartition = new CopyOnWriteArrayList<>();
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> committed = new AtomicReference<>();
        doAnswer(invocation -> {
            assertEquals("Final commit must follow the in-flight handler's completion", 0, handlerFinished.getCount());
            committed.set(Map.copyOf(invocation.getArgument(0)));
            return null;
        }).when(consumer).commitSync(anyMap(), any(Duration.class));
        doAnswer(invocation -> {
            assertNotNull("Kafka close must follow the partial-success commit", committed.get());
            return null;
        }).when(consumer).close(any(Duration.class));
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> {
            if (record.partition() == 1) {
                inFlight.countDown();
                awaitLatch(release);
                assertFalse(Thread.currentThread().isInterrupted());
                handlerFinished.countDown();
            } else {
                handledFirstPartition.add(record.offset());
                if (record.offset() == 1) {
                    awaitLatch(inFlight);
                    failed.countDown();
                    throw fatal;
                }
            }
            return ConsumeStatus.OK;
        }), 2000, () -> { created.incrementAndGet(); return consumer; });
        client.start();
        KafkaConsumerExecution<?, ?> execution = run(client);
        try {
            assertTrue(failed.await(2, TimeUnit.SECONDS));
            verify(consumer, never()).close(any(Duration.class));
            verify(consumer, never()).commitSync(anyMap(), any(Duration.class));
            release.countDown();
            await().atMost(Duration.ofSeconds(2)).until(() -> pollingExecutor(client).isTerminated());
            assertSame(fatal, assertThrows(AssertionError.class, client::stop));
            assertTrue(execution.isFullyStopped());
            assertEquals(Map.of(first, new OffsetAndMetadata(1), second, new OffsetAndMetadata(6)), committed.get());
            assertEquals(List.of(0L, 1L), handledFirstPartition);
            assertEquals(1, created.get());
            var order = inOrder(consumer);
            order.verify(consumer).commitSync(anyMap(), eq(Duration.ofSeconds(3)));
            order.verify(consumer).close(Duration.ofSeconds(3));
        } finally {
            release.countDown();
            client.stop();
        }
    }

    @Test
    public void recovery_retries_old_consumer_close_before_constructing_replacement_on_same_polling_thread() throws Exception {
        KafkaConsumer<String, String> first = mock(KafkaConsumer.class);
        KafkaConsumer<String, String> replacement = mock(KafkaConsumer.class);
        AtomicReference<Thread> pollingThread = new AtomicReference<>();
        when(first.poll(any(Duration.class))).thenAnswer(invocation -> {
            pollingThread.set(Thread.currentThread());
            throw new WakeupException();
        });
        AtomicInteger closes = new AtomicInteger();
        doAnswer(invocation -> {
            assertSame(pollingThread.get(), Thread.currentThread());
            if (closes.incrementAndGet() <= 2) {
                throw new IllegalStateException("retry close before replace");
            }
            return null;
        }).when(first).close(any(Duration.class));
        CountDownLatch polledReplacement = new CountDownLatch(1);
        CountDownLatch wake = new CountDownLatch(1);
        when(replacement.poll(any(Duration.class))).thenAnswer(invocation -> {
            assertSame(pollingThread.get(), Thread.currentThread());
            polledReplacement.countDown();
            awaitLatch(wake);
            throw new WakeupException();
        });
        doAnswer(invocation -> { wake.countDown(); return null; }).when(replacement).wakeup();
        AtomicInteger created = new AtomicInteger();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK),
                2000, () -> {
                    if (created.incrementAndGet() == 1) {
                        return first;
                    }
                    assertEquals(3, closes.get());
                    assertSame(pollingThread.get(), Thread.currentThread());
                    return replacement;
                });
        client.start();
        try {
            assertTrue(polledReplacement.await(2, TimeUnit.SECONDS));
            assertEquals(2, created.get());
        } finally {
            client.stop();
        }
    }

    @Test
    public void rebalance_retries_pending_offsets_from_a_completed_batch_directly_on_execution() throws Exception {
        KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
        AtomicReference<ConsumerRebalanceListener> listener = new AtomicReference<>();
        doAnswer(invocation -> { listener.set(invocation.getArgument(1)); return null; })
                .when(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
        doThrow(new IllegalStateException("first commit")).doNothing()
                .when(consumer).commitSync(anyMap(), any(Duration.class));
        CountDownLatch rebalanced = new CountDownLatch(1);
        CountDownLatch wake = new CountDownLatch(1);
        AtomicInteger polls = new AtomicInteger();
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            if (polls.incrementAndGet() == 1) {
                return records();
            }
            listener.get().onPartitionsRevoked(List.of(new TopicPartition("topic", 0)));
            rebalanced.countDown();
            awaitLatch(wake);
            throw new WakeupException();
        });
        doAnswer(invocation -> { wake.countDown(); return null; }).when(consumer).wakeup();
        KafkaConsumerClientImpl<String, String> client = new KafkaConsumerClientImpl<>(config(record -> ConsumeStatus.OK),
                2000, () -> consumer);
        client.start();
        try {
            assertTrue(rebalanced.await(2, TimeUnit.SECONDS));
            assertSame(run(client), listener.get());
            verify(consumer, times(2)).commitSync(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)),
                    Duration.ofSeconds(3));
        } finally {
            client.stop();
        }
        verify(consumer, times(2)).commitSync(anyMap(), any(Duration.class));
    }

    private static KafkaConsumerClientImpl<String, String> client(TopicConsumer<String, String> handler, long timeout) {
        return new KafkaConsumerClientImpl<>(config(handler), timeout);
    }

    private static ExecutorService pollingExecutor(KafkaConsumerClientImpl<?, ?> client) throws Exception {
        Object handle = handle(client);
        Field field = handle.getClass().getDeclaredField("pollingExecutor");
        field.setAccessible(true);
        return (ExecutorService) field.get(handle);
    }

    private static KafkaConsumerExecution.Phase phase(KafkaConsumerExecution<?, ?> run) throws Exception {
        Field state = KafkaConsumerExecution.class.getDeclaredField("state");
        state.setAccessible(true);
        Object snapshot = state.get(run);
        Field phase = snapshot.getClass().getDeclaredField("phase");
        phase.setAccessible(true);
        return (KafkaConsumerExecution.Phase) phase.get(snapshot);
    }

    private static KafkaConsumerClientConfig<String, String> config(TopicConsumer<String, String> handler) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumerClientConfig<>(properties, Map.of("topic", handler));
    }

    private static MockedConstruction<KafkaConsumer> blockingConsumers() {
        return blockingConsumers(consumer -> {});
    }

    private static MockedConstruction<KafkaConsumer> blockingConsumers(Consumer<KafkaConsumer> configure) {
        return mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            blockPoll(consumer);
            configure.accept(consumer);
        });
    }

    private static void blockPoll(KafkaConsumer consumer) {
        CountDownLatch wakeup = new CountDownLatch(1);
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            awaitLatch(wakeup);
            throw new WakeupException();
        });
        doAnswer(invocation -> {
            wakeup.countDown();
            return null;
        }).when(consumer).wakeup();
    }

    private static MockedConstruction<KafkaConsumer> recordConsumers() {
        return recordConsumers(consumer -> {});
    }

    private static MockedConstruction<KafkaConsumer> recordConsumers(Consumer<KafkaConsumer> configure) {
        return mockConstruction(KafkaConsumer.class, (consumer, context) -> {
            when(consumer.poll(any(Duration.class))).thenReturn(records()).thenThrow(new WakeupException());
            configure.accept(consumer);
        });
    }

    private static ConsumerRecords<String, String> records() {
        return new ConsumerRecords<>(Map.of(new TopicPartition("topic", 0),
                List.of(new ConsumerRecord<>("topic", 0, 0, "key", "value"))));
    }

    private static Object handle(KafkaConsumerClientImpl<?, ?> client) throws Exception {
        Field field = KafkaConsumerClientImpl.class.getDeclaredField("activeExecution");
        field.setAccessible(true);
        return field.get(client);
    }

    private static KafkaConsumerExecution<?, ?> run(KafkaConsumerClientImpl<?, ?> client) throws Exception {
        Object handle = handle(client);
        Field field = handle.getClass().getDeclaredField("execution");
        field.setAccessible(true);
        return (KafkaConsumerExecution<?, ?>) field.get(handle);
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue("Latch was not released", latch.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Handler/poller was interrupted", e);
        }
    }
}
