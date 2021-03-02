package no.nav.common.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;

public interface KafkaProducerListener {

    default void onSuccess(ProducerRecord<String, String> record) {}

    default void onError(ProducerRecord<String, String> record, Exception exception) {}

}
