package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

public class PostgresConsumerRepository<K, V> implements KafkaConsumerRepository<K, V> {

    private final DataSource dataSource;

    private final Serializer<K> keySerializer;
    private final Deserializer<K> keyDeserializer;

    private final Serializer<V> valueSerializer;
    private final Deserializer<V> valueDeserializer;

    public PostgresConsumerRepository(
            DataSource dataSource,
            Serializer<K> keySerializer,
            Deserializer<K> keyDeserializer,
            Serializer<V> valueSerializer,
            Deserializer<V> valueDeserializer
    ) {
        this.dataSource = dataSource;
        this.keySerializer = keySerializer;
        this.keyDeserializer = keyDeserializer;
        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public long storeRecord(ConsumerRecord<K, V> record) {
        return 0;
    }

    @Override
    public void deleteRecord(long id) {

    }

    @Override
    public List<KafkaConsumerRecord<K, V>> getRecords(List<String> topics, Instant olderThan, int maxMessages) {
        return null;
    }
}
