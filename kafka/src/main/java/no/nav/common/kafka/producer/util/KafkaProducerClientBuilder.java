package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;

import java.util.Properties;

public class KafkaProducerClientBuilder<K, V> {

    private Properties properties;

    private MeterRegistry meterRegistry;

    private KafkaProducerClientBuilder() {}

    public static <K, V> KafkaProducerClientBuilder<K, V> builder() {
        return new KafkaProducerClientBuilder<>();
    }

    public KafkaProducerClientBuilder<K, V> withProperties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public KafkaProducerClientBuilder<K, V> withMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }

    public KafkaProducerClient<K, V> build() {
        if (properties == null) {
            throw new IllegalStateException("Cannot build kafka producer without properties");
        }

        KafkaProducerClient<K, V> producerClient = new KafkaProducerClientImpl<>(properties);

        if (meterRegistry != null) {
            producerClient = new KafkaProducerClientWithMetrics<>(producerClient, meterRegistry);
        }

        return producerClient;
    }

}
