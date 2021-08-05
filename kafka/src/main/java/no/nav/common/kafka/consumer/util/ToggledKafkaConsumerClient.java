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
public class ToggledKafkaConsumerClient implements KafkaConsumerClient {

    private final static Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(1);

    private final KafkaConsumerClient kafkaConsumerClient;

    private final Duration pollTimeoutDuration;

    private final Supplier<Boolean> toggleForStoppingConsumersSupplier;

    private ScheduledExecutorService executorService;

    public ToggledKafkaConsumerClient(KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOnSupplier) {
        this.kafkaConsumerClient = kafkaConsumerClient;
        this.toggleForStoppingConsumersSupplier = isToggledOnSupplier;
        this.pollTimeoutDuration = DEFAULT_POLL_TIMEOUT;
    }

    public ToggledKafkaConsumerClient(KafkaConsumerClient kafkaConsumerClient, Supplier<Boolean> isToggledOnSupplier, Duration pollTimeoutDuration) {
        this.kafkaConsumerClient = kafkaConsumerClient;
        this.toggleForStoppingConsumersSupplier = isToggledOnSupplier;
        this.pollTimeoutDuration = pollTimeoutDuration;
    }

    @Override
    public void start() {
        if (!isRunning()) {
            kafkaConsumerClient.start();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::syncRunningStateWithToggle, pollTimeoutDuration.toMillis(), pollTimeoutDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
        }

        if (isRunning()) {
            kafkaConsumerClient.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return kafkaConsumerClient.isRunning();
    }

    private void syncRunningStateWithToggle() {
        boolean isToggleOnForConsumersStopping = toggleForStoppingConsumersSupplier.get();
        boolean isRunning = isRunning();

        if (isToggleOnForConsumersStopping && isRunning) {
            log.info("Stopping consumer... Toggle for stopping consumers is on and kafka consumer client is running");
            kafkaConsumerClient.stop();
        } else if (!isToggleOnForConsumersStopping && !isRunning) {
            log.info("Starting consumer... Toggle for stopping consumers is off and kafka consumer client is not running");
            kafkaConsumerClient.start();
        }
    }
}
