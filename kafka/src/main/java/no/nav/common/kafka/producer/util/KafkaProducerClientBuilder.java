package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;

import java.util.Properties;

public class KafkaProducerClientBuilder<K, V> {

    private Properties properties;

    private Properties additionalProperties;

    private MeterRegistry meterRegistry;

    private KafkaProducerClientBuilder() {}

    public static <K, V> KafkaProducerClientBuilder<K, V> builder() {
        return new KafkaProducerClientBuilder<>();
    }

    public KafkaProducerClientBuilder<K, V> withProperties(@NonNull Properties properties) {
        this.properties = (Properties) properties.clone();
        return this;
    }

    /**
     * Adds additional properties that will overwrite properties from {@link #withProperties(Properties)}.
     * Useful for configuring a producer with additional properties when using a preset from
     * {@link no.nav.common.kafka.util.KafkaPropertiesPreset} as the base.
     * @param properties additional properties
     * @return this builder
     */
    public KafkaProducerClientBuilder<K, V> withAdditionalProperties(@NonNull Properties properties) {
        this.additionalProperties = (Properties) properties.clone();
        return this;
    }

    /**
     * Adds an additional property that will overwrite properties from {@link #withProperties(Properties)}.
     * Useful for configuring a producer with additional properties when using a preset from
     * {@link no.nav.common.kafka.util.KafkaPropertiesPreset} as the base.
     * @param name property name
     * @param value property value
     * @return this builder
     */
    public KafkaProducerClientBuilder<K, V> withAdditionalProperty(@NonNull String name, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new Properties();
        }

        additionalProperties.put(name, value);
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

        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }

        return createClient(properties, meterRegistry);
    }

    private KafkaProducerClient<K, V> createClient(Properties properties, MeterRegistry meterRegistry) {
        KafkaProducerClient<K, V> producerClient = new KafkaProducerClientImpl<>(properties);

        if (meterRegistry != null) {
            producerClient = new KafkaProducerClientWithMetrics<>(producerClient, meterRegistry);
        }

        return producerClient;
    }

}
