package no.nav.common.kafka.feilhandtering;

import no.nav.common.kafka.domain.KafkaMessage;
import no.nav.common.kafka.feilhandtering.db.KafkaRepository;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.KafkaProducerClientImpl;
import no.nav.common.kafka.util.KafkaProducerUtils;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

public class StoreAndForwardProducer {
    private final KafkaRepository repository;
    private final KafkaProducerClient<String, String> client;

    public StoreAndForwardProducer(KafkaRepository repository, KafkaProducerClient<String, String> client) {
        this.repository = repository;
        this.client = client;
    }

    public StoreAndForwardProducer(KafkaRepository repository, Properties properties) {
        this.repository = repository;
        this.client = new KafkaProducerClientImpl<>(properties);
    }

    public void send(String key, String record, String topic) {
        long id = repository.toBeSent(topic, record, key);

        send(id, topic, key, record);
    }

    public void resendFailedMessages() {
        List<KafkaMessage> messages = repository.getUnsentOltherThan(Instant.now().minusSeconds(10));
        messages.forEach(m -> send(m.id, m.topic, m.key, m.value));
    }


    private void send(long id, String topic, String key, String record) {
        client.send(KafkaProducerUtils.toRecord(topic, key, record), (metadata, exception) -> {
            if (exception == null) {
                repository.sentOk(id);
            } else {
                repository.failed(id);
            }
        });
    }
}
