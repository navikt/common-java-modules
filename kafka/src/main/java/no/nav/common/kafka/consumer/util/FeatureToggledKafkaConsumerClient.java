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
 */
@Slf4j
public class FeatureToggledKafkaConsumerClient implements KafkaConsumerClient {

    private final static Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(1);

    private final KafkaConsumerClient kafkaConsumerClient;

    private final Duration pollTimeoutDuration;

    private final Supplier<Boolean> isConsumerToggledOffSupplier;

    private ScheduledExecutorService executorService;

    private volatile boolean active;

    private boolean cleanupRequired;

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
        if (active) {
            return;
        }
        if (cleanupRequired) {
            throw new IllegalStateException("Previous toggle lifecycle has not stopped; call stop() again");
        }
        cleanupRequired = true;
        active = true;
        try {
            boolean isConsumerToggledOff = isConsumerToggledOffSupplier.get();
            if (!isRunning() && !isConsumerToggledOff) {
                kafkaConsumerClient.start();
            }
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> {
                try {
                    syncRunningStateWithToggle();
                } catch (RuntimeException e) {
                    log.error("Failed to synchronize kafka consumer with feature toggle; will retry", e);
                }
            }, pollTimeoutDuration.toMillis(), pollTimeoutDuration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException | Error failure) {
            try {
                stop();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }
    
    @Override
    public synchronized void stop() {
        active = false;
        shutAndAwaitExecutor();

        if (cleanupRequired) {
            kafkaConsumerClient.stop();
            cleanupRequired = false;
        }
    }

    @Override
    public boolean isRunning() {
        return kafkaConsumerClient.isRunning();
    }

    private void syncRunningStateWithToggle() {
        if (!active) {
            return;
        }
        boolean isConsumerToggledOff = isConsumerToggledOffSupplier.get();
        if (!active) {
            return;
        }
        boolean isRunning = isRunning();
        if (!active) {
            return;
        }

        if (isConsumerToggledOff && isRunning) {
            log.info("Stopping consumer... Toggle for stopping consumers is on and kafka consumer client is running");
            kafkaConsumerClient.stop();
        } else if (!isConsumerToggledOff && !isRunning) {
            log.info("Starting consumer... Toggle for stopping consumers is off and kafka consumer client is not running");
            try {
                kafkaConsumerClient.start();
            } catch (RuntimeException | Error failure) {
                try {
                    kafkaConsumerClient.stop();
                } catch (RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != failure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            }
        }
    }

    private void shutAndAwaitExecutor() {
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
