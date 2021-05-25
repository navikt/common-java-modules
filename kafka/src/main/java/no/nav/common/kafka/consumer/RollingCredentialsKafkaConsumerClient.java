package no.nav.common.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.util.ConsumerClientExceptionListener;
import org.apache.kafka.common.errors.AuthenticationException;

import java.util.List;

@Slf4j
public class RollingCredentialsKafkaConsumerClient implements KafkaConsumerClient, ConsumerClientExceptionListener {

    private final KafkaConsumerClientConfig<?, ?> consumerClientConfig;

    private volatile KafkaConsumerClient kafkaConsumerClient;

    public RollingCredentialsKafkaConsumerClient(KafkaConsumerClientConfig<?, ?> consumerClientConfig) {
        this.consumerClientConfig = consumerClientConfig;
        consumerClientConfig.setExceptionListeners(List.of(this));
        kafkaConsumerClient = new KafkaConsumerClientImpl<>(consumerClientConfig);
    }

    @Override
    public void start() {
        kafkaConsumerClient.start();
    }

    @Override
    public void stop() {
        kafkaConsumerClient.stop();
    }

    @Override
    public boolean isRunning() {
        return kafkaConsumerClient.isRunning();
    }

    @Override
    public void onExceptionCaught(Exception e) {
        if (e instanceof AuthenticationException) {
            log.info("Received AuthenticationException when consuming from topic. Recreating consumer client");
            kafkaConsumerClient.stop();
            kafkaConsumerClient = new KafkaConsumerClientImpl<>(consumerClientConfig);
            kafkaConsumerClient.start();
        }
    }
}
