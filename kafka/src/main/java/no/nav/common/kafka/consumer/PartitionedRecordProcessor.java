package no.nav.common.kafka.consumer;

import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Ordered record handling only; Kafka resource ownership belongs to the execution. */
final class PartitionedRecordProcessor<K, V> implements AutoCloseable {

    /**
     * The owner retains this result before dispatch, including on dispatch/await failure,
     * and through drain. Successful handlers may still publish offsets during drain.
     */
    static final class Batch<K, V> {
        private final ConsumerRecords<K, V> records;
        private final Map<TopicPartition, OffsetAndMetadata> successfulOffsets = new ConcurrentHashMap<>();
        private final Set<TopicPartition> failedPartitions = ConcurrentHashMap.newKeySet();
        private final AtomicReference<Error> fatalFailure = new AtomicReference<>();

        Batch(ConsumerRecords<K, V> records) {
            this.records = records;
        }

        Map<TopicPartition, OffsetAndMetadata> successfulOffsets() {
            return Map.copyOf(successfulOffsets);
        }

        void acknowledgeCommit() {
            successfulOffsets.clear();
        }

        Set<TopicPartition> failedPartitions() {
            return Set.copyOf(failedPartitions);
        }

        void failPartitions(Iterable<TopicPartition> partitions) {
            partitions.forEach(failedPartitions::add);
        }

        void rethrowFatalFailure() {
            Error failure = fatalFailure.get();
            if (failure != null) {
                throw failure;
            }
        }
    }

    private final Map<String, TopicConsumer<K, V>> handlers;
    private final Map<TopicPartition, ExecutorService> partitionWorkers = new HashMap<>();
    private boolean acceptingBatches = true;

    PartitionedRecordProcessor(Map<String, TopicConsumer<K, V>> handlers) {
        this.handlers = handlers;
    }

    /** Lazily creates and reuses one dedicated thread per encountered topic-partition. */
    void processBatch(Batch<K, V> batch, BooleanSupplier processingAllowed) throws InterruptedException {
        if (!acceptingBatches) {
            throw new IllegalStateException("Record processor has been closed");
        }
        ArrayList<Future<?>> tasks = new ArrayList<>();
        for (TopicPartition partition : batch.records.partitions()) {
            try {
                ExecutorService worker = partitionWorkers.computeIfAbsent(partition,
                        ignored -> Executors.newSingleThreadExecutor());
                tasks.add(worker.submit(() -> {
                    try {
                        for (var record : batch.records.records(partition)) {
                            if (!processingAllowed.getAsBoolean() || batch.failedPartitions.contains(partition)) {
                                break;
                            }
                            if (ConsumerUtils.safeConsume(handlers.get(record.topic()), record) != ConsumeStatus.OK) {
                                batch.failedPartitions.add(partition);
                                break;
                            }
                            /*
                                From KafkaConsumer.commitSync documentation:

                                The committed offset should be the next message your application will consume, i.e.
                                lastProcessedMessageOffset + 1.
                             */
                            batch.successfulOffsets.put(partition, new OffsetAndMetadata(record.offset() + 1));
                        }
                    } catch (RuntimeException | Error failure) {
                        batch.failedPartitions.add(partition);
                        if (failure instanceof Error error) {
                            batch.fatalFailure.compareAndSet(null, error);
                        }
                        throw failure;
                    }
                }));
            } catch (RuntimeException | Error failure) {
                batch.failedPartitions.add(partition);
                if (failure instanceof Error error) {
                    batch.fatalFailure.compareAndSet(null, error);
                }
                throw failure;
            }
        }
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (ExecutionException failure) {
                if (failure.getCause() instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Partition processing failed", failure.getCause());
            }
        }
    }

    /**
     * Idempotently stops intake and drains accepted work without interrupting handlers.
     * Restores caller interruption after draining; a stuck handler can block indefinitely.
     * Batch outcomes remain readable. Does not commit offsets or close Kafka.
     */
    @Override
    public void close() {
        acceptingBatches = false;
        partitionWorkers.values().forEach(ExecutorService::shutdown);
        boolean interrupted = false;
        for (ExecutorService worker : partitionWorkers.values()) {
            while (!worker.isTerminated()) {
                try {
                    worker.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException failure) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
