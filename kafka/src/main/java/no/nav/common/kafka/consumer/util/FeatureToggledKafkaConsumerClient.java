package no.nav.common.kafka.consumer.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * {@link KafkaConsumerClient} with support for toggling the consumer on/off based off the result from a {@link Supplier}
 * Use cases for this implementation is for example when there is a need to pause the consumer to change the offset and restart the client.
 *
 * <p>STOPPED -> STARTING -> ACTIVE -> STOPPING -> STOPPED.
 * ACTIVE means the toggle scheduler is enabled, not that the underlying consumer
 * is polling. Failed startup/shutdown remains STOPPING until cleanup succeeds.
 */
@Slf4j
public class FeatureToggledKafkaConsumerClient implements KafkaConsumerClient {

    private final static Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(1);

    private final KafkaConsumerClient kafkaConsumerClient;

    private final Duration pollTimeoutDuration;

    private final Supplier<Boolean> isConsumerToggledOffSupplier;

    private ScheduledExecutorService executorService;

    private enum State {
        STOPPED, STARTING, ACTIVE, STOPPING
    }

    private volatile State state = State.STOPPED;

    private final long shutdownTimeoutMillis;

    public FeatureToggledKafkaConsumerClient(KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOffSupplier) {
        this(kafkaConsumerClient, isToggledOffSupplier, DEFAULT_POLL_TIMEOUT);
    }

    public FeatureToggledKafkaConsumerClient(KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOffSupplier, Duration pollTimeoutDuration) {
        this(kafkaConsumerClient, isToggledOffSupplier, pollTimeoutDuration, TimeUnit.SECONDS.toMillis(10));
    }

    FeatureToggledKafkaConsumerClient(KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOffSupplier,
                                     Duration pollTimeoutDuration, long shutdownTimeoutMillis) {
        this.kafkaConsumerClient = kafkaConsumerClient;
        this.isConsumerToggledOffSupplier = isToggledOffSupplier;
        this.pollTimeoutDuration = pollTimeoutDuration;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    @Override
    public synchronized void start() {
        if (state == State.ACTIVE) {
            return;
        }
        if (state != State.STOPPED) {
            throw new IllegalStateException("Previous toggle lifecycle has not stopped; call stop() again");
        }
        state = State.STARTING;
        try {
            applyInitialToggle();
            state = State.ACTIVE;
            startToggleScheduler();
        } catch (RuntimeException | Error failure) {
            rollbackStartup(failure);
            throw failure;
        }
    }
    
    @Override
    public synchronized void stop() {
        if (state == State.STOPPED) {
            return;
        }
        state = State.STOPPING;
        stopToggleScheduler();
        kafkaConsumerClient.stop();
        state = State.STOPPED;
    }

    @Override
    public boolean isRunning() {
        return kafkaConsumerClient.isRunning();
    }

    private void applyInitialToggle() {
        boolean disabled = isConsumerToggledOffSupplier.get();
        if (!isRunning() && !disabled) {
            kafkaConsumerClient.start();
        }
    }

    private void startToggleScheduler() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::pollToggle,
                pollTimeoutDuration.toMillis(), pollTimeoutDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void rollbackStartup(Throwable startupFailure) {
        try {
            stop();
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != startupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
        }
    }

    /**
     * Scheduler entry point. It never takes the lifecycle monitor: stop() waits
     * for this task to exit before stopping the underlying client.
     */
    private void pollToggle() {
        try {
            syncRunningStateWithToggle();
        } catch (RuntimeException e) {
            log.error("Failed to synchronize kafka consumer with feature toggle; will retry", e);
        }
    }

    private void syncRunningStateWithToggle() {
        if (state != State.ACTIVE) {
            return;
        }
        boolean isConsumerToggledOff = isConsumerToggledOffSupplier.get();
        if (state != State.ACTIVE) {
            return;
        }
        boolean isRunning = isRunning();
        if (state != State.ACTIVE) {
            return;
        }

        if (isConsumerToggledOff && isRunning) {
            log.info("Stopping consumer... Toggle for stopping consumers is on and kafka consumer client is running");
            kafkaConsumerClient.stop();
        } else if (!isConsumerToggledOff && !isRunning) {
            log.info("Starting consumer... Toggle for stopping consumers is off and kafka consumer client is not running");
            resumeConsumer();
        }
    }

    private void resumeConsumer() {
        try {
            kafkaConsumerClient.start();
        } catch (RuntimeException | Error startFailure) {
            try {
                kafkaConsumerClient.stop();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != startFailure) {
                    startFailure.addSuppressed(cleanupFailure);
                }
            }
            throw startFailure;
        }
    }

    private void stopToggleScheduler() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(shutdownTimeoutMillis, TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Timed out stopping kafka feature toggle scheduler");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while stopping kafka feature toggle scheduler", e);
            }
        }
    }
}
