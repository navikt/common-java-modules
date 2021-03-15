package no.nav.common.kafka.producer.util;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.StoreAndForwardProducer;

import java.util.Properties;

public class KafkaProducerClientBuilder<K, V> {

    private Properties properties;

    private MeterRegistry meterRegistry;

    private KafkaProducerRepository<K, V> kafkaProducerRepository;

    private KafkaProducerClientBuilder() {}

    public static <K, V> KafkaProducerClientBuilder<K, V> builder() {
        return new KafkaProducerClientBuilder<K, V>();
    }

    public KafkaProducerClientBuilder<K, V> withProps(Properties properties) {
        this.properties = properties;
        return this;
    }

    public KafkaProducerClientBuilder<K, V> withMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        return this;
    }

    public KafkaProducerClientBuilder<K, V> withStoreAndForward(KafkaProducerRepository<K, V> kafkaProducerRepository) {
        this.kafkaProducerRepository = kafkaProducerRepository;
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

        if (kafkaProducerRepository != null) {
            producerClient = new StoreAndForwardProducer<>(producerClient, kafkaProducerRepository);
        }

        return producerClient;
    }

}
