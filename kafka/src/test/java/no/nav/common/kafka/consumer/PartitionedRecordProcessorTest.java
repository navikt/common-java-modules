package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PartitionedRecordProcessorTest {

    @Test
    public void construction_has_no_worker_or_kafka_side_effects() {
        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of());
            executors.verifyNoInteractions();
            processor.close();
            processor.close();
            assertThrows(IllegalStateException.class, () -> processor.processBatch(batch(0, 0), () -> true));
            executors.verifyNoInteractions();
        }
        for (var field : PartitionedRecordProcessor.class.getDeclaredFields()) {
            assertFalse(field.getGenericType().getTypeName().contains("KafkaConsumer"));
        }
        for (var constructor : PartitionedRecordProcessor.class.getDeclaredConstructors()) {
            for (var type : constructor.getGenericParameterTypes()) {
                assertFalse(type.getTypeName().contains("KafkaConsumer"));
            }
        }
    }

    @Test
    public void records_are_ordered_failure_skips_partition_and_next_batch_retries_on_same_thread() throws Exception {
        List<Long> handled = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean(true);
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of("topic", record -> {
            handled.add(record.offset());
            threads.add(Thread.currentThread());
            return record.offset() == 1 && fail.get() ? ConsumeStatus.FAILED : ConsumeStatus.OK;
        }));
        TopicPartition partition = new TopicPartition("topic", 0);
        try (processor) {
            var first = batch(0, 0, 1, 2);
            processor.processBatch(first, () -> true);
            assertEquals(List.of(0L, 1L), handled);
            assertEquals(Set.of(partition), first.failedPartitions());
            assertEquals(Map.of(partition, new OffsetAndMetadata(1)), first.successfulOffsets());
            fail.set(false);
            var retry = batch(0, 1, 2);
            processor.processBatch(retry, () -> true);
            assertEquals(List.of(0L, 1L, 1L, 2L), handled);
            assertEquals(Map.of(partition, new OffsetAndMetadata(3)), retry.successfulOffsets());
            threads.forEach(thread -> assertSame(threads.getFirst(), thread));
        }
        for (Thread thread : threads) {
            thread.join(2000);
            assertFalse(thread.isAlive());
        }
        assertThrows(IllegalStateException.class, () -> processor.processBatch(batch(0, 3), () -> true));
    }

    @Test
    public void partitions_run_concurrently_on_distinct_reused_threads() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        Map<Integer, Thread> threads = new java.util.concurrent.ConcurrentHashMap<>();
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of("topic", record -> {
            Thread previous = threads.putIfAbsent(record.partition(), Thread.currentThread());
            if (previous != null) {
                assertSame(previous, Thread.currentThread());
            }
            bothEntered.countDown();
            awaitLatch(bothEntered);
            return ConsumeStatus.OK;
        }));
        var records = new ConsumerRecords<>(Map.of(
                new TopicPartition("topic", 0), List.of(record(0, 0)),
                new TopicPartition("topic", 1), List.of(record(1, 0))));
        try (processor) {
            processor.processBatch(new PartitionedRecordProcessor.Batch<>(records), () -> true);
            processor.processBatch(new PartitionedRecordProcessor.Batch<>(records), () -> true);
            assertNotSame(threads.get(0), threads.get(1));
        }
        for (Thread thread : threads.values()) {
            thread.join(2000);
            assertFalse(thread.isAlive());
        }
    }

    @Test
    public void abnormal_processing_keeps_successful_offsets_and_marks_partition_failed() {
        AtomicInteger checks = new AtomicInteger();
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(
                Map.of("topic", record -> ConsumeStatus.OK));
        var batch = batch(0, 0, 1, 2);
        try (processor) {
            assertThrows(IllegalStateException.class, () -> processor.processBatch(batch, () -> {
                if (checks.incrementAndGet() == 2) {
                    throw new IllegalStateException("cancellation source failed");
                }
                return true;
            }));
        }
        assertEquals(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)), batch.successfulOffsets());
        assertEquals(Set.of(new TopicPartition("topic", 0)), batch.failedPartitions());
    }

    @Test
    public void dispatch_failure_keeps_results_of_already_dispatched_work_after_scope_close() {
        ExecutorService accepted = Executors.newSingleThreadExecutor();
        ExecutorService rejected = mock(ExecutorService.class);
        when(rejected.isTerminated()).thenReturn(true);
        when(rejected.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException("dispatch"));
        var records = new ConsumerRecords<>(Map.of(
                new TopicPartition("topic", 0), List.of(record(0, 0)),
                new TopicPartition("topic", 1), List.of(record(1, 0))));
        var batch = new PartitionedRecordProcessor.Batch<>(records);
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(
                Map.of("topic", record -> ConsumeStatus.OK));
        try (MockedStatic<Executors> executors = mockStatic(Executors.class, CALLS_REAL_METHODS)) {
            executors.when(Executors::newSingleThreadExecutor).thenReturn(accepted, rejected);
            assertThrows(RejectedExecutionException.class, () -> {
                try (processor) {
                    processor.processBatch(batch, () -> true);
                }
            });
            assertTrue(accepted.isTerminated());
            assertEquals(1, batch.successfulOffsets().size());
            assertEquals(1, batch.failedPartitions().size());
            assertEquals(1, batch.successfulOffsets().values().iterator().next().offset());
        } finally {
            accepted.shutdown();
        }
    }

    @Test
    public void interrupted_await_closes_scope_without_interrupting_in_flight_success() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Thread> callerThread = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicBoolean allowed = new AtomicBoolean(true);
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of("topic", record -> {
            entered.countDown();
            awaitLatch(release);
            assertFalse(Thread.currentThread().isInterrupted());
            return ConsumeStatus.OK;
        }));
        var batch = batch(0, 0, 1);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            var task = caller.submit(() -> {
                callerThread.set(Thread.currentThread());
                assertThrows(InterruptedException.class, () -> {
                    try (processor) {
                        try {
                            processor.processBatch(batch, allowed::get);
                        } catch (InterruptedException failure) {
                            allowed.set(false);
                            interrupted.countDown();
                            throw failure;
                        }
                    }
                });
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            callerThread.get().interrupt();
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
            assertFalse(task.isDone());
            release.countDown();
            task.get(2, TimeUnit.SECONDS);
            assertEquals(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)), batch.successfulOffsets());
        } finally {
            release.countDown();
            caller.shutdown();
            assertTrue(caller.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    public void fatal_handler_failure_is_observable_and_does_not_lose_prior_success() {
        AssertionError fatal = new AssertionError("handler");
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of("topic", record -> {
            if (record.offset() == 1) {
                throw fatal;
            }
            return ConsumeStatus.OK;
        }));
        var batch = batch(0, 0, 1, 2);
        try (processor) {
            assertSame(fatal, assertThrows(AssertionError.class, () -> processor.processBatch(batch, () -> true)));
        }
        assertEquals(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)), batch.successfulOffsets());
        assertSame(fatal, assertThrows(AssertionError.class, batch::rethrowFatalFailure));
    }

    @Test
    public void exceptional_scope_close_restores_interrupt_and_never_interrupts_handler() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closing = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Thread> owner = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Thread> handler = new java.util.concurrent.atomic.AtomicReference<>();
        PartitionedRecordProcessor<String, String> processor = new PartitionedRecordProcessor<>(Map.of("topic", record -> {
            handler.set(Thread.currentThread());
            entered.countDown();
            awaitLatch(release);
            assertFalse(Thread.currentThread().isInterrupted());
            return ConsumeStatus.OK;
        }));
        var batch = batch(0, 0);
        IllegalStateException bodyFailure = new IllegalStateException("scope body");
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            var task = caller.submit(() -> {
                owner.set(Thread.currentThread());
                assertSame(bodyFailure, assertThrows(IllegalStateException.class, () -> {
                    try (processor) {
                        try {
                            processor.processBatch(batch, () -> true);
                        } catch (InterruptedException failure) {
                            Thread.currentThread().interrupt();
                            closing.countDown();
                            throw bodyFailure;
                        }
                    }
                }));
                assertTrue(Thread.currentThread().isInterrupted());
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            owner.get().interrupt();
            assertTrue(closing.await(2, TimeUnit.SECONDS));
            assertFalse(task.isDone());
            release.countDown();
            task.get(2, TimeUnit.SECONDS);
            handler.get().join(2000);
            assertFalse(handler.get().isAlive());
            assertEquals(Map.of(new TopicPartition("topic", 0), new OffsetAndMetadata(1)), batch.successfulOffsets());
            assertThrows(IllegalStateException.class, () -> processor.processBatch(batch, () -> true));
        } finally {
            release.countDown();
            caller.shutdown();
            assertTrue(caller.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    private static PartitionedRecordProcessor.Batch<String, String> batch(int partition, long... offsets) {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        for (long offset : offsets) {
            records.add(record(partition, offset));
        }
        return new PartitionedRecordProcessor.Batch<>(
                new ConsumerRecords<>(Map.of(new TopicPartition("topic", partition), records)));
    }

    private static ConsumerRecord<String, String> record(int partition, long offset) {
        return new ConsumerRecord<>("topic", partition, offset, "key", "value");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue("Latch was not released", latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Handler was interrupted", failure);
        }
    }
}
