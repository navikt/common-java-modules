package no.nav.common.kafka.listener;

import no.nav.common.kafka.db.KafkaRepository;
import no.nav.common.kafka.producer.KafkaProducerListener;
import org.apache.kafka.clients.producer.ProducerRecord;

public class DatabaseStorageProducerErrorListener implements KafkaProducerListener {

    private final KafkaRepository kafkaRepository;

    public DatabaseStorageProducerErrorListener(KafkaRepository kafkaRepository) {
        this.kafkaRepository = kafkaRepository;
    }

    @Override
    public void onError(ProducerRecord<String, String> record, Exception exception) {
        // TODO: Lagre noen greier her ved bruk av kafkaRepository
    }

}
