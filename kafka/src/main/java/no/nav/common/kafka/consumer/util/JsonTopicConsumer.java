package no.nav.common.kafka.consumer.util;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.function.BiFunction;

import static no.nav.common.json.JsonUtils.fromJson;

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
        T jsonValue = record.value() != null
                ? fromJson(assertedString(record.value()), dataClass)
                : null;

        return consumer.apply(record, jsonValue);
    }

    private static String assertedString(Object obj) {
        if (!(obj instanceof String)) {
            throw new IllegalStateException("JsonTopicConsumer can only consume topics where the value is a String");
        }

        return (String) obj;
    }

}
