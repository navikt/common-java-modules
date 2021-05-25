package no.nav.common.kafka.consumer;

public interface KafkaConsumerClient {

    void start();

    void stop();

    boolean isRunning();

}
