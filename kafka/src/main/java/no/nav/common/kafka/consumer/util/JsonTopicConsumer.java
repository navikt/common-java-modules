package no.nav.common.kafka.consumer.util;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.function.Function;

public class JsonTopicConsumer<T> implements TopicConsumer<String, String> {

    private final Function<T, ConsumeStatus> consumer;

    private final Class<T> dataClass;

    public JsonTopicConsumer(Class<T> dataClass, Function<T, ConsumeStatus> consumer) {
        this.dataClass = dataClass;
        this.consumer = consumer;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> record) {
        return consumer.apply(JsonUtils.fromJson(record.value(), dataClass));
    }

}
