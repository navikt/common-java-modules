package no.nav.common.kafka.feilhandtering.db;

import no.nav.common.kafka.domain.KafkaProducerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

public class PostgresProducerRepository<K, V> implements KafkaProducerRepository<K, V> {

    private final DataSource dataSource;

    private final Serializer<K> keySerializer;
    private final Deserializer<K> keyDeserializer;

    private final Serializer<V> valueSerializer;
    private final Deserializer<V> valueDeserializer;

    public PostgresProducerRepository(
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
    public long storeRecord(ProducerRecord<K, V> record) {
        return 0;
    }

    @Override
    public void deleteRecord(long id) {

    }

    @Override
    public List<KafkaProducerRecord<K, V>> getRecords(List<String> topics, Instant olderThan, int maxMessages) {
        return null;
    }

}
