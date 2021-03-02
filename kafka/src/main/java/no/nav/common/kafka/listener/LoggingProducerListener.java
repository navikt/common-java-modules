package no.nav.common.kafka.listener;

import no.nav.common.kafka.producer.KafkaProducerListener;
import org.apache.kafka.clients.producer.ProducerRecord;

public class LoggingProducerListener implements KafkaProducerListener {

    @Override
    public void onSuccess(ProducerRecord<String, String> record) {
        // TODO: Log
    }

    @Override
    public void onError(ProducerRecord<String, String> record, Exception exception) {
        // TODO: Log
    }

}
