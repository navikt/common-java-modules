package no.nav.common.kafka.consumer;

public interface KafkaConsumerClient {

    void start();

    /**
     * Stops this client and waits for shutdown to complete.
     * Throws {@link IllegalStateException} on timeout or interruption; callers may retry.
     * In-flight user handlers are not interrupted, so they may outlive a failed stop.
     */
    void stop();

    boolean isRunning();

}
