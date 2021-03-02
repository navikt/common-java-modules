package no.nav.common.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface KafkaConsumerListener {

    void consume(ConsumerRecord<String, String> record);

    // TODO: Might want to split this interface in two, because there can only be 1 consume() but several onSuccess() and onError()

    default void onSuccess(ConsumerRecord<String, String> record) {}

    default void onError(ConsumerRecord<String, String> record, Exception exception) {}

}
