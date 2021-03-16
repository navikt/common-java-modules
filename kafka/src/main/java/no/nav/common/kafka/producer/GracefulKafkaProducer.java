package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

/**
 * Adds graceful shutdown to {@link KafkaProducer}}
 * @param <K> topic key
 * @param <V> topic value
 */
public class GracefulKafkaProducer<K, V> extends KafkaProducer<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public GracefulKafkaProducer(Properties properties) {
        super(properties);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void shutdown() {
        log.info("Shutting down kafka producer...");

        try {
            close(Duration.ofSeconds(3));
            log.info("Kafka producer was shut down successfully");
        } catch (Exception e) {
            log.error("Failed to shut down kafka producer gracefully", e);
        }
    }

}
