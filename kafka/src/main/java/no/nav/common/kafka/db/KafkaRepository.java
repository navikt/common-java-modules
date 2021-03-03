package no.nav.common.kafka.db;

import no.nav.common.kafka.domain.KafkaMessage;

import java.time.Instant;
import java.util.List;

public interface KafkaRepository {
    long toBeSent(String topic, String record, String key);

    void sentOk(long id);

    void failed(long id);

    List<KafkaMessage> getUnsentOltherThan(Instant minusSeconds);

    // TODO: Add some default functions here for storing messages

}
