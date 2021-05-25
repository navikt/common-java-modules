package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder which makes it easier to compose a {@link no.nav.common.kafka.consumer.TopicConsumer} with additional functionality
 * such as logging and metrics
 * @param <K> topic key
 * @param <V> topic value
 */
public class TopicConsumerBuilder<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final List<TopicConsumerListener<K, V>> listeners = new ArrayList<>();

    private TopicConsumer<K, V> consumer;

    private TopicConsumerBuilder() {}

    public static <K, V> TopicConsumerBuilder<K, V> builder() {
        return new TopicConsumerBuilder<>();
    }

    public TopicConsumerBuilder<K, V> withConsumer(TopicConsumer<K, V> consumer) {
        this.consumer = consumer;
        return this;
    }

    public TopicConsumerBuilder<K, V> withLogging() {
        listeners.add(new TopicConsumerLogger<>());
        return this;
    }

    public TopicConsumerBuilder<K, V> withMetrics(MeterRegistry meterRegistry) {
        listeners.add(new TopicConsumerMetrics<>(meterRegistry));
        return this;
    }

    public TopicConsumerBuilder<K, V> withListener(TopicConsumerListener<K, V> consumerListener) {
        listeners.add(consumerListener);
        return this;
    }

    public TopicConsumerBuilder<K, V> withListeners(List<TopicConsumerListener<K, V>> consumerListeners) {
        listeners.addAll(consumerListeners);
        return this;
    }

    public TopicConsumer<K, V> build() {
        if (consumer == null) {
            throw new IllegalStateException("Cannot build TopicConsumer without consumer");
        }

        TopicConsumer<K, V> topicConsumer = (record) -> {
            ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);
            listeners.forEach(listener -> {
                try {
                    listener.onConsumed(record, status);
                } catch (Exception e) {
                    log.error("Caught exception from consumer listener", e);
                }
            });
            return status;
        };

        return topicConsumer;
    }

}
