package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaMessage;

import java.time.Instant;
import java.util.List;

public interface KafkaRepository {
    long storeProducerMessage(String topic, String key, String value);

    long storeConsumerMessage(String topic, String record, String key, long offset, int parition);

    void deleteProducerMessage(long id);

    void deleteConsumerMessage(long id);

    void failed(long id);

    List<KafkaMessage> getUnsentOlderThan(Instant minusSeconds);

    // TODO: Add some default functions here for storing messages

}
