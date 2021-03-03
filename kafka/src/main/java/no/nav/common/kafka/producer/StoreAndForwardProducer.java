package no.nav.common.kafka.producer;

import no.nav.common.kafka.db.KafkaRepository;
import no.nav.common.kafka.domain.KafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class StoreAndForwardProducer {
    private final KafkaRepository repository;
    private final KafkaProducerClient client;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public StoreAndForwardProducer(KafkaRepository repository, KafkaProducerClient client) {
        this.repository = repository;
        this.client = client;
    }

    public StoreAndForwardProducer(KafkaRepository repository, KafkaProducerClientConfig config) {
        this.repository = repository;
        this.client = new KafkaProducerClient(config);
    }

    public void send(String key, String record, String topic) {
        long id = repository.toBeSent(topic, record, key);

        send(id, topic, key, record);
    }

    private void send(long id, String topic, String key, String record) {
        client.sendAsync(KafkaProducerUtils.toRecord(topic, key, record), (metadata, exception) -> {
            if(exception == null) {
                repository.sentOk(id);
            } else {
                repository.failed(id);
            }
        });
    }

    private void send(KafkaMessage message) {
        send(message.id, message.topic, message.key, message.value);
    }

    public void resendFailedMessages() {
        List<KafkaMessage> messages =  repository.getUnsentOltherThan(Instant.now().minusSeconds(10));
        messages.forEach(this::send);
    }
}
