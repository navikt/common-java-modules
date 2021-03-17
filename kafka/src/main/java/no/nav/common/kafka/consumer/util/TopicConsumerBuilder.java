package no.nav.common.kafka.consumer.util;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    public <T> TopicConsumerBuilder<K, V> withJsonConsumer(Class<T> jsonType, Function<T, ConsumeStatus> consumer) {
        this.consumer = new JsonTopicConsumer<>(jsonType, consumer);
        return this;
    }

    public <T> TopicConsumerBuilder<K, V> withJsonConsumer(Class<T> jsonType, BiFunction<ConsumerRecord<K, V>, T, ConsumeStatus> consumer) {
        this.consumer = new JsonTopicConsumer<>(jsonType, consumer);
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
