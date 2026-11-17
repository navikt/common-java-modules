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
            ExecutorService firstExecutor = executor(client);
            client.start();
            assertEquals(1, consumers.constructed().size());

            client.stop();
            client.stop();
            assertTrue(firstExecutor.isTerminated());
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            assertFalse(client.isRunning());

            client.start();
            assertEquals(2, consumers.constructed().size());
            assertNotSame(firstExecutor, executor(client));
            client.stop();
            assertTrue(executor(client).isTerminated());
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
    public void failed_submission_shuts_down_executor_and_closes_consumer() {
        ExecutorService rejectedExecutor = mock(ExecutorService.class);
        RejectedExecutionException failure = new RejectedExecutionException("submit");
        when(rejectedExecutor.submit(any(Runnable.class))).thenThrow(failure);
        try (MockedConstruction<KafkaConsumer> consumers = mockConstruction(KafkaConsumer.class);
             MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(rejectedExecutor);
            KafkaConsumerClientImpl<String, String> client = client(record -> ConsumeStatus.OK, 1000);
            assertSame(failure, assertThrows(RejectedExecutionException.class, client::start));
            verify(rejectedExecutor).shutdown();
            verify(consumers.constructed().get(0)).close(Duration.ofSeconds(3));
            assertFalse(client.isRunning());
            when(rejectedExecutor.isTerminated()).thenReturn(true);
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
                assertTrue(executor(client).isTerminated());
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
                await().atMost(Duration.ofSeconds(2)).until(() -> executor(client).isTerminated());
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
        ExecutorService worker = executor(client);
        try {
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            assertTrue(client.isRunning());
            assertSame(worker, executor(client));
            verify(first, times(2)).poll(any(Duration.class));
            verify(first).commitSync(anyMap(), any(Duration.class));
            verify(first).close(Duration.ofSeconds(3));
            verify(second).subscribe(anyCollection(), same(client));
        } finally {
            client.stop();
        }
        assertTrue(worker.isTerminated());
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
        try {
            assertTrue(restarted.await(2, TimeUnit.SECONDS));
            assertTrue(client.isRunning());
            assertArrayEquals(new Throwable[]{cleanupFailure}, startFailure.getSuppressed());
            verify(failed, never()).poll(any(Duration.class));
            verify(failed, times(2)).close(Duration.ofSeconds(3));
        } finally {
            client.stop();
        }
        assertTrue(executor(client).isTerminated());
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
            KafkaConsumer consumer = consumers.constructed().get(0);
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                client.stop();
                verify(consumer, times(2)).commitSync(anyMap(), any(Duration.class));
                verify(consumer, times(2)).close(Duration.ofSeconds(3));
                assertArrayEquals(new Throwable[]{closeFailure}, commitFailure.getSuppressed());
                assertTrue(executor(client).isTerminated());
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
        try {
            assertTrue(replacing.await(2, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, client::stop);
            assertThrows(IllegalStateException.class, client::start);
            assertThrows(IllegalStateException.class, client::stop);
        } finally {
            release.countDown();
            await().atMost(Duration.ofSeconds(2)).until(() -> executor(client).isTerminated());
            client.stop();
        }
        assertEquals(2, created.get());
        verify(first).close(Duration.ofSeconds(3));
        verify(second).close(Duration.ofSeconds(3));
        verify(second, never()).poll(any(Duration.class));
        verify(second, never()).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
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

    private static KafkaConsumerClientImpl<String, String> client(TopicConsumer<String, String> handler, long timeout) {
        return new KafkaConsumerClientImpl<>(config(handler), timeout);
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

    private static ExecutorService executor(KafkaConsumerClientImpl<?, ?> client) throws Exception {
        Field field = KafkaConsumerClientImpl.class.getDeclaredField("pollExecutor");
        field.setAccessible(true);
        return (ExecutorService) field.get(client);
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
