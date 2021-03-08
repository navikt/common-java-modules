package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaMessage;

import java.time.Instant;
import java.util.List;

public class OracleKafkaRepository implements KafkaRepository {

    @Override
    public long storeProducerMessage(String topic, String record, String key) {
        return 0;
    }

    @Override
    public long storeConsumerMessage(String topic, String record, String key, long offset, int parition) {
        return 0;
    }

    @Override
    public void deleteProducerMessage(long id) {

    }

    @Override
    public void deleteConsumerMessage(long id) {

    }

    @Override
    public void failed(long id) {

    }

    @Override
    public List<KafkaMessage> getUnsentOlderThan(Instant minusSeconds) {
        return null;
    }
}
