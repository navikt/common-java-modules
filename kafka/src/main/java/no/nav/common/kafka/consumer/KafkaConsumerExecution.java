package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One execution, not a lifecycle controller. CURRENT STATE is an immutable snapshot.
 * EVENTS originate in the caller (initialize, shutdown, terminated cleanup handoff)
 * or worker (recovery, failure). No blocking operation holds the transition lock.
 */
final class KafkaConsumerExecution<K, V> implements ConsumerRebalanceListener {

    enum Phase { NEW, STARTING, CONSUMING, RECOVERING, SHUTTING_DOWN, CLEANUP_REQUIRED, STOPPED }

    private record State<K, V>(Phase phase, Resources<K, V> resources, Throwable failure) {}
    record WorkerExit<K, V>(Resources<K, V> unclosedResources, Throwable failure) {}
    private record RecoveryOutcome<K, V>(Resources<K, V> resources, Throwable failure) {}

    /** Opaque worker input and cleanup handoff; only the execution accesses Kafka. */
    static final class Resources<K, V> {
        private final KafkaConsumer<K, V> consumer;
        private final PartitionedRecordProcessor<K, V> processor;
        private final Map<TopicPartition, OffsetAndMetadata> pendingOffsets = new HashMap<>();
        private PartitionedRecordProcessor.Batch<K, V> batch;
        private volatile boolean closed;

        private Resources(KafkaConsumer<K, V> consumer, Map<String, TopicConsumer<K, V>> handlers) {
            this.consumer = consumer;
            this.processor = new PartitionedRecordProcessor<>(handlers);
        }
    }

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration COMMIT_TIMEOUT = Duration.ofSeconds(3);
    private final Logger log = LoggerFactory.getLogger(KafkaConsumerExecution.class);
    private final KafkaConsumerClientConfig<K, V> config;
    private final Supplier<KafkaConsumer<K, V>> consumerFactory;
    private final Object transitionLock = new Object();
    private volatile State<K, V> state = new State<>(Phase.NEW, null, null);

    KafkaConsumerExecution(KafkaConsumerClientConfig<K, V> config, Supplier<KafkaConsumer<K, V>> consumerFactory) {
        this.config = config;
        this.consumerFactory = consumerFactory;
    }

    /** Caller event: initialization is synchronous, without creating any worker. */
    Resources<K, V> initialize() {
        synchronized (transitionLock) {
            if (state.phase() != Phase.NEW) {
                throw new IllegalStateException("Execution has already been initialized");
            }
            state = new State<>(Phase.STARTING, null, null);
        }
        Resources<K, V> resources = null;
        try {
            resources = createResources();
            resources.consumer.subscribe(new ArrayList<>(config.topics.keySet()), this);
            acceptInitialized(resources, Phase.STARTING);
            return resources;
        } catch (RuntimeException | Error failure) {
            startupFailed(resources, failure);
            throw failure;
        }
    }

    private Resources<K, V> createResources() {
        Resources<K, V> resources = new Resources<>(consumerFactory.get(), config.topics);
        synchronized (transitionLock) {
            state = new State<>(state.phase(), resources, state.failure());
        }
        return resources;
    }

    private void acceptInitialized(Resources<K, V> resources, Phase expected) {
        synchronized (transitionLock) {
            if (state.phase() == expected) {
                state = new State<>(Phase.CONSUMING, resources, null);
            }
        }
    }

    /** Caller event: failed initialization or submission retains any unclosed resource. */
    void startupFailed(Resources<K, V> resources, Throwable failure) {
        // No worker scope was entered, so startup still owns the unused processor.
        if (resources != null) {
            resources.processor.close();
        }
        addSuppressed(failure, close(resources, CLOSE_TIMEOUT));
        synchronized (transitionLock) {
            state = new State<>(Phase.CLEANUP_REQUIRED, unclosed(resources), null);
        }
    }

    void observeStartupTermination() {
        synchronized (transitionLock) {
            if (state.resources() == null) {
                state = new State<>(Phase.STOPPED, null, null);
            }
        }
    }

    /** Caller event: disable processing/recovery before waking the published resource. */
    void requestShutdown() {
        Resources<K, V> resources;
        synchronized (transitionLock) {
            if (state.phase() == Phase.SHUTTING_DOWN || state.phase() == Phase.CLEANUP_REQUIRED
                    || state.phase() == Phase.STOPPED) {
                return;
            }
            state = new State<>(Phase.SHUTTING_DOWN, state.resources(), state.failure());
            resources = state.resources();
        }
        if (resources != null && !resources.closed) {
            resources.consumer.wakeup();
        }
    }

    boolean isRunning() {
        Phase phase = state.phase();
        return phase == Phase.CONSUMING || phase == Phase.RECOVERING;
    }

    boolean isFullyStopped() {
        return state.phase() == Phase.STOPPED;
    }

    /** The sole worker entry point; initial ownership is explicit, never fetched from state. */
    WorkerExit<K, V> execute(Resources<K, V> initialInput) {
        Resources<K, V> resources = initialInput;
        Throwable failure = null;
        try {
            // Even shutdown before the first poll must enter the processor's owning scope.
            do {
                pollAndProcess(resources);
                Throwable cleanupFailure = commitAndClose(resources);
                if (cleanupFailure instanceof Error) {
                    failure = cleanupFailure;
                    break;
                }
                var wasInConsumerPhase = beginRecovery();
                if (!wasInConsumerPhase) break;

                RecoveryOutcome<K, V> recovery = recover(resources);
                resources = recovery.resources();
                if (recovery.failure() != null) {
                    failure = recovery.failure();
                    break;
                }
            } while (resources != null && !resources.closed);
        } catch (RuntimeException | Error workerFailure) {
            failure = workerFailure;
            beginWorkerShutdown(failure);
            failure = combine(failure, commitAndClose(resources));
        } finally {
            beginWorkerShutdown(failure);
            if (!(failure instanceof Error)) {
                failure = combine(failure, close(resources, CLOSE_TIMEOUT));
            }
            beginWorkerShutdown(failure);
        }
        rethrowError(failure);
        return new WorkerExit<>(unclosed(resources), failure);
    }

    private void pollAndProcess(Resources<K, V> resources) {
        try (PartitionedRecordProcessor<K, V> processor = resources.processor) {
            pollUntilExit(resources, processor);
        } catch (WakeupException failure) {
            log.info("Kafka polling was woken");
        } catch (Exception failure) {
            log.error("Unexpected processing exit", failure);
        }
    }

    private void pollUntilExit(Resources<K, V> resources, PartitionedRecordProcessor<K, V> processor)
            throws InterruptedException {
        try {
            while (isRunning()) {
                ConsumerRecords<K, V> records;
                try {
                    records = resources.consumer.poll(Duration.ofMillis(config.pollDurationMs));
                } catch (WakeupException failure) {
                    throw failure;
                } catch (Exception failure) {
                    log.error("Polling failed; waiting before retry", failure);
                    Thread.sleep(5000);
                    continue;
                }
                if (records.isEmpty()) {
                    continue;
                }
                Map<TopicPartition, OffsetAndMetadata> firstOffsets = new HashMap<>();
                records.forEach(record -> firstOffsets.merge(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset()), (first, next) -> first.offset() <= next.offset() ? first : next));
                seek(resources, firstOffsets);
                resources.batch = new PartitionedRecordProcessor.Batch<>(records);
                processor.processBatch(resources.batch, this::isRunning);
                commitAndLogFailure(resources);
                // Completed batches transfer failed commits to pendingOffsets; abnormal exits retain the batch.
                resources.batch = null;
            }
        } catch (Error failure) {
            // Disable unstarted handlers before the enclosing scope drains in-flight work.
            beginWorkerShutdown(failure);
            throw failure;
        }
    }

    private boolean beginRecovery() {
        synchronized (transitionLock) {
            if (state.phase() != Phase.CONSUMING) {
                return false;
            }
            state = new State<>(Phase.RECOVERING, state.resources(), null);
            return true;
        }
    }

    private RecoveryOutcome<K, V> recover(Resources<K, V> previous) {
        Resources<K, V> candidate = previous;
        while (isRunning()) {
            Throwable closeFailure = close(candidate, CLOSE_TIMEOUT);
            if (closeFailure instanceof Error) {
                return new RecoveryOutcome<>(candidate, closeFailure);
            }
            if (unclosed(candidate) != null) {
                continue;
            }
            if (!isRunning()) {
                break;
            }
            candidate = null;
            try {
                candidate = createResources();
                if (!isRunning()) {
                    break;
                }
                candidate.consumer.subscribe(new ArrayList<>(config.topics.keySet()), this);
                acceptInitialized(candidate, Phase.RECOVERING);
                return new RecoveryOutcome<>(candidate, null);
            } catch (RuntimeException | Error failure) {
                // Replacement initialization failed before transferring to a worker scope.
                if (candidate != null) {
                    candidate.processor.close();
                }
                Throwable cleanupFailure = close(candidate, CLOSE_TIMEOUT);
                addSuppressed(failure, cleanupFailure);
                if (failure instanceof Error) {
                    return new RecoveryOutcome<>(candidate, failure);
                }
                if (cleanupFailure instanceof Error) {
                    addSuppressed(cleanupFailure, failure);
                    return new RecoveryOutcome<>(candidate, cleanupFailure);
                }
                log.error("Failed to initialize replacement Kafka consumer", failure);
            }
        }
        return new RecoveryOutcome<>(candidate, null);
    }

    private void beginWorkerShutdown(Throwable failure) {
        synchronized (transitionLock) {
            state = new State<>(Phase.SHUTTING_DOWN, state.resources(), failure);
        }
    }

    /** The polling scope has drained the processor; retained batch outcomes are now final. */
    private Throwable commitAndClose(Resources<K, V> resources) {
        if (resources == null || resources.closed) {
            return null;
        }
        Throwable failure = null;
        try {
            if (resources.batch != null) {
                resources.batch.rethrowFatalFailure();
            }
        } catch (Error fatal) {
            failure = fatal;
        }
        Throwable commitFailure = null;
        try {
            commitProcessedOffsets(resources);
        } catch (RuntimeException | Error commitError) {
            commitFailure = commitError;
            log.error("Final Kafka offset commit failed", commitError);
        }
        Throwable closeFailure = close(resources, CLOSE_TIMEOUT);
        addSuppressed(commitFailure, closeFailure);
        if (commitFailure instanceof Error) {
            failure = combine(failure, commitFailure);
        }
        return combine(failure, closeFailure);
    }

    private Throwable close(Resources<K, V> resources, Duration timeout) {
        if (resources == null || resources.closed) {
            return null;
        }
        try {
            resources.consumer.close(timeout);
            resources.closed = true;
            return null;
        } catch (RuntimeException | Error failure) {
            log.error("Failed to close Kafka consumer", failure);
            return failure;
        }
    }

    WorkerExit<K, V> failedTask(Throwable taskFailure) {
        State<K, V> current = state;
        return new WorkerExit<>(unclosed(current.resources()), combine(current.failure(), taskFailure));
    }

    /** Caller event, permitted only after observing both task and executor termination. */
    void completeAfterTermination(WorkerExit<K, V> exit, Duration closeTimeout) {
        Resources<K, V> resources = exit.unclosedResources();
        Throwable cleanupFailure = close(resources, closeTimeout);
        synchronized (transitionLock) {
            state = new State<>(unclosed(resources) == null ? Phase.STOPPED : Phase.CLEANUP_REQUIRED,
                    unclosed(resources), null);
        }
        rethrow(combine(exit.failure(), cleanupFailure));
    }

    private void commitAndLogFailure(Resources<K, V> resources) {
        try {
            commitProcessedOffsets(resources);
        } catch (Exception failure) {
            log.error("Failed to commit Kafka offsets", failure);
        }
    }

    private void commitProcessedOffsets(Resources<K, V> resources) {
        if (resources.batch != null) {
            resources.pendingOffsets.putAll(resources.batch.successfulOffsets());
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.copyOf(resources.pendingOffsets);
        if (!offsets.isEmpty()) {
            seek(resources, offsets);
            resources.consumer.commitSync(offsets, COMMIT_TIMEOUT);
            resources.pendingOffsets.clear();
            if (resources.batch != null) {
                resources.batch.acknowledgeCommit();
            }
        }
    }

    private void seek(Resources<K, V> resources, Map<TopicPartition, OffsetAndMetadata> offsets) {
        offsets.forEach((partition, offset) -> {
            try {
                resources.consumer.seek(partition, offset);
            } catch (IllegalStateException failure) {
                log.warn("Unable to seek unassigned partition {} to {}", partition, offset);
            }
        });
    }

    /** Normal Kafka callbacks are registered directly on this execution. */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        Resources<K, V> resources = state.resources();
        if (resources != null && !resources.closed) {
            if (resources.batch != null) {
                resources.batch.failPartitions(partitions);
            }
            commitAndLogFailure(resources);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("Kafka partitions assigned: {}", partitions);
    }

    private static <K, V> Resources<K, V> unclosed(Resources<K, V> resources) {
        return resources != null && !resources.closed ? resources : null;
    }

    static Throwable combine(Throwable primary, Throwable additional) {
        if (primary == null) {
            return additional;
        }
        if (!(primary instanceof Error) && additional instanceof Error) {
            addSuppressed(additional, primary);
            return additional;
        }
        addSuppressed(primary, additional);
        return primary;
    }

    private static void addSuppressed(Throwable primary, Throwable additional) {
        if (primary != null && additional != null && primary != additional) {
            primary.addSuppressed(additional);
        }
    }

    private static void rethrowError(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private static void rethrow(Throwable failure) {
        rethrowError(failure);
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failure != null) {
            throw new IllegalStateException("Kafka execution failed", failure);
        }
    }
}
