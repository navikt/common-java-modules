package no.nav.common.kafka.db;

import java.util.Map;

public class OracleKafkaRepository implements KafkaRepository {

    @Override
    public long toBeSent(String topic, String record, String key) {
        return 0;
    }

    @Override
    public void sentOk(long id) {

    }

    @Override
    public void failed(long id) {

    }
}
