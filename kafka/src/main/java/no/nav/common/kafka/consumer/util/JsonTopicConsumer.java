package no.nav.common.kafka.consumer.util;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Topic consumer which deserializes JSON messages from topics.
 * @param <K> topic key
 * @param <V> topic value (NB: This must always be a String! A generic type is used to make JsonTopicConsumer compatible with the other generic kafka components)
 * @param <T> deserialized JSON
 */
public class JsonTopicConsumer<K, V, T> implements TopicConsumer<K, V> {

    private final BiFunction<ConsumerRecord<K, V>, T, ConsumeStatus> consumer;

    private final Class<T> dataClass;

    public JsonTopicConsumer(Class<T> dataClass, BiFunction<ConsumerRecord<K, V>, T, ConsumeStatus> consumer) {
        this.dataClass = dataClass;
        this.consumer = consumer;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<K, V> record) {
        String stringValue = assertedString(record.value());
        return consumer.apply(record, JsonUtils.fromJson(stringValue, dataClass));
    }

    public static <K, V, D> JsonTopicConsumer<K, V, D> jsonConsumer(Class<D> dataClass, Function<D, ConsumeStatus> consumer) {
        return new JsonTopicConsumer<>(dataClass, (k, t) -> consumer.apply(t));
    }

    public static <K, V, D> JsonTopicConsumer<K, V, D> jsonConsumer(Class<D> dataClass, Consumer<D> consumer) {
        return new JsonTopicConsumer<>(dataClass, (record, data) -> {
            consumer.accept(data);
            return ConsumeStatus.OK;
        });
    }

    public static <K, V, D> JsonTopicConsumer<K, V, D> jsonConsumer(Class<D> dataClass, BiConsumer<ConsumerRecord<K, V>, D> consumer) {
        return new JsonTopicConsumer<>(dataClass, (record, data) -> {
            consumer.accept(record, data);
            return ConsumeStatus.OK;
        });
    }

    private static String assertedString(Object obj) {
        if (!(obj instanceof String)) {
            throw new IllegalStateException("JsonTopicConsumer can only consume topics where the value is a String");
        }

        return (String) obj;
    }

}
