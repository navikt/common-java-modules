package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Lifecycle controller for one {@link KafkaConsumerExecution} at a time.
 * start()/stop() serialize ownership changes; workers never read or modify this client.
 * A failed stop retains the execution, preventing replacement until cleanup completes.
 */
public class KafkaConsumerClientImpl<K, V> implements KafkaConsumerClient, ConsumerRebalanceListener {

    public static final long DEFAULT_POLL_DURATION_MS = 1000;

    private final KafkaConsumerClientConfig<K, V> config;
    private final long shutdownTimeoutMillis;
    private final Supplier<KafkaConsumer<K, V>> consumerFactory;
    private record Handle<K, V>(KafkaConsumerExecution<K, V> execution, ExecutorService pollingExecutor,
                                 FutureTask<KafkaConsumerExecution.WorkerExit<K, V>> task) {}

    private volatile Handle<K, V> activeExecution;

    public KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config) {
        this(config, TimeUnit.SECONDS.toMillis(10));
    }

    KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, long shutdownTimeoutMillis) {
        this(config, shutdownTimeoutMillis, () -> new KafkaConsumer<>(config.properties));
    }

    KafkaConsumerClientImpl(KafkaConsumerClientConfig<K, V> config, long shutdownTimeoutMillis,
                            Supplier<KafkaConsumer<K, V>> consumerFactory) {
        validateConfig(config);
        this.config = config;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
        this.consumerFactory = consumerFactory;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public synchronized void start() {
        if (activeExecution != null && activeExecution.execution().isFullyStopped()) {
            activeExecution = null;
        }
        if (activeExecution != null) {
            if (activeExecution.execution().isRunning()) {
                return;
            }
            throw new IllegalStateException("Previous kafka consumer execution has not stopped; call stop() again");
        }

        KafkaConsumerExecution<K, V> execution = new KafkaConsumerExecution<>(config, consumerFactory);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        KafkaConsumerExecution.Resources<K, V> initialInput = null;
        try {
            initialInput = execution.initialize();
            launchPollingThread(execution, executor, initialInput);
        } catch (RuntimeException | Error failure) {
            executor.shutdown();
            // initialize() has already rolled back its own failure.
            if (initialInput != null) {
                execution.startupFailed(initialInput, failure);
            }
            activeExecution = new Handle<>(execution, executor, null);
            if (executor.isTerminated()) {
                execution.observeStartupTermination();
                if (execution.isFullyStopped()) {
                    activeExecution = null;
                }
            }
            throw failure;
        }
    }

    /** The controller alone creates and submits the actual polling task. */
    private void launchPollingThread(KafkaConsumerExecution<K, V> execution, ExecutorService executor,
                                     KafkaConsumerExecution.Resources<K, V> initialInput) {
        FutureTask<KafkaConsumerExecution.WorkerExit<K, V>> task = new FutureTask<>(() -> {
            try {
                return execution.execute(initialInput);
            } finally {
                executor.shutdown();
            }
        });
        activeExecution = new Handle<>(execution, executor, task);
        executor.execute(task);
    }

    @Override
    public synchronized void stop() {
        Handle<K, V> handle = activeExecution;
        if (handle == null) {
            return;
        }
        if (handle.execution().isFullyStopped()) {
            activeExecution = null;
            return;
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(shutdownTimeoutMillis);
        handle.execution().requestShutdown();
        handle.pollingExecutor().shutdown();
        KafkaConsumerExecution.WorkerExit<K, V> exit;
        try {
            if (handle.task() == null) {
                exit = handle.execution().failedTask(null);
            } else {
                try {
                    exit = handle.task().get(remainingTime(deadline), TimeUnit.NANOSECONDS);
                } catch (ExecutionException failure) {
                    exit = handle.execution().failedTask(failure.getCause());
                }
            }
            if (!handle.pollingExecutor().awaitTermination(remainingTime(deadline), TimeUnit.NANOSECONDS)) {
                throw new TimeoutException("Polling executor is still running");
            }
        } catch (TimeoutException failure) {
            throw new IllegalStateException("Timed out stopping kafka consumer client; processing may still be in progress", failure);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping kafka consumer client", failure);
        }
        // The terminated task's failure is observed once; later stops retry only retained cleanup.
        activeExecution = new Handle<>(handle.execution(), handle.pollingExecutor(), null);
        handle.execution().completeAfterTermination(exit,
                Duration.ofNanos(Math.min(TimeUnit.SECONDS.toNanos(3), remainingTime(deadline))));
        activeExecution = null;
    }

    @Override
    public boolean isRunning() {
        Handle<K, V> handle = activeExecution;
        return handle != null && handle.execution().isRunning();
    }

    /** Compatibility hook only; normal Kafka callbacks go directly to the execution. */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        legacyRebalanceListener().onPartitionsRevoked(partitions);
    }

    /** Compatibility hook only; normal Kafka callbacks go directly to the execution. */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        legacyRebalanceListener().onPartitionsAssigned(partitions);
    }

    private ConsumerRebalanceListener legacyRebalanceListener() {
        Handle<K, V> handle = activeExecution;
        if (handle == null) {
            throw new IllegalStateException("No active execution is available for rebalance callbacks");
        }
        return handle.execution();
    }

    private static long remainingTime(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }

    private static void validateConfig(KafkaConsumerClientConfig<?, ?> config) {
        if (config.topics.isEmpty()) {
            throw new IllegalArgumentException("\"topics\" must contain at least 1 topic");
        }
        if (config.pollDurationMs <= 0) {
            throw new IllegalArgumentException("\"pollDurationMs\" must be larger than 0");
        }
        if (!Boolean.FALSE.equals(config.properties.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG))) {
            throw new IllegalArgumentException(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG + " must be false!");
        }
    }
}
