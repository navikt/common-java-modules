package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Properties;
import lombok.NonNull;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;

@SuppressWarnings("unused")
public class KafkaProducerClientBuilder<K, V> {

    private Properties properties;

    private Properties additionalProperties;

    private MeterRegistry meterRegistry;

    private boolean registerShutdownHook = true;

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

    @SuppressWarnings("UnusedReturnValue")
    public KafkaProducerClientBuilder<K, V> withMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }

    /**
     * Velger om produceren skal registrere en JVM-shutdown-hook. Standardverdien er true.
     *
     * <p>Ved false må applikasjonen kalle close, direkte eller gjennom publisheren som eier klienten.
     * Valget gjelder også ved bruk av withMetrics.
     *
     * @param registerShutdownHook true for automatisk lukking ved JVM-shutdown
     * @return denne builderen
     */
    public KafkaProducerClientBuilder<K, V> withShutdownHookEnabled(boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
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
        KafkaProducerClient<K, V> producerClient = new KafkaProducerClientImpl<>(properties, registerShutdownHook);

        if (meterRegistry != null) {
            producerClient = new KafkaProducerClientWithMetrics<>(producerClient, meterRegistry);
        }

        return producerClient;
    }
}
