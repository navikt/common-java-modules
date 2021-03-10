package no.nav.common.kafka.feilhandtering.db;

import lombok.SneakyThrows;
import no.nav.common.kafka.domain.KafkaConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.feilhandtering.db.Constants.*;
import static no.nav.common.kafka.feilhandtering.db.DatabaseUtils.*;

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

    @SneakyThrows
    @Override
    public long storeRecord(ConsumerRecord<K, V> record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)",
                CONSUMER_RECORD_TABLE, ID, TOPIC, PARTITION, RECORD_OFFSET, KEY, VALUE
        );

        long id = incrementAndGetPostgresSequence(dataSource, CONSUMER_RECORD_ID_SEQ);

        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            statement.setLong(1, id);
            statement.setString(2, record.topic());
            statement.setInt(3, record.partition());
            statement.setLong(4, record.offset());
            statement.setBytes(5, keySerializer.serialize(record.topic(), record.key()));
            statement.setBytes(6, valueSerializer.serialize(record.topic(), record.value()));
            statement.executeUpdate();
        }

        return id;
    }

    @SneakyThrows
    @Override
    public void deleteRecord(long id) {
        String sql = format("DELETE FROM %s WHERE %s = ?", CONSUMER_RECORD_TABLE, ID);
        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public List<KafkaConsumerRecord<K, V>> getRecords(List<String> topics, int maxMessages) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ANY(?) LIMIT %d",
                CONSUMER_RECORD_TABLE, TOPIC, maxMessages
        );

        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            Array array = dataSource.getConnection().createArrayOf("VARCHAR", topics.toArray());
            statement.setArray(1, array);
            return fetchConsumerRecords(statement.executeQuery(), keyDeserializer, valueDeserializer);
        }
    }

}
