package no.nav.common.kafka.listener;

import no.nav.common.kafka.producer.KafkaProducerListener;
import org.apache.kafka.clients.producer.ProducerRecord;

public class MetricProducerListener implements KafkaProducerListener {

    @Override
    public void onSuccess(ProducerRecord<String, String> record) {
        // TODO: Increment some metric
    }

    @Override
    public void onError(ProducerRecord<String, String> record, Exception exception) {
        // TODO: Increment some metric
    }

}
