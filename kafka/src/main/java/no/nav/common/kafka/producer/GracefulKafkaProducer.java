package no.nav.common.kafka.producer;

import java.time.Duration;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds graceful shutdown to {@link KafkaProducer}}
 *
 * @param <K> topic key
 * @param <V> topic value
 */
public class GracefulKafkaProducer<K, V> extends KafkaProducer<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("FieldCanBeLocal")
    private final Thread shutdownHook;

    public GracefulKafkaProducer(Properties properties) {
        this(properties, true);
    }

    /**
     * Oppretter en producer med valgfri JVM-shutdown-hook.
     *
     * @param properties           Kafka-konfigurasjon
     * @param registerShutdownHook true for automatisk lukking; ved false må applikasjonen kalle close
     */
    public GracefulKafkaProducer(Properties properties, boolean registerShutdownHook) {
        super(properties);
        shutdownHook = registerShutdownHook ? new Thread(this::shutdown) : null;
        if (shutdownHook != null) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
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
