package no.nav.common.kafka.feilhandtering.db;

import lombok.SneakyThrows;
import no.nav.common.kafka.domain.KafkaProducerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.feilhandtering.db.Constants.*;
import static no.nav.common.kafka.feilhandtering.db.DatabaseUtils.*;

public class OracleProducerRepository<K, V> implements KafkaProducerRepository<K, V> {

    private final DataSource dataSource;

    private final Serializer<K> keySerializer;
    private final Deserializer<K> keyDeserializer;

    private final Serializer<V> valueSerializer;
    private final Deserializer<V> valueDeserializer;

    public OracleProducerRepository(
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
    public long storeRecord(ProducerRecord<K, V> record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                PRODUCER_RECORD_TABLE, ID, TOPIC, KEY, VALUE
        );

        long id = incrementAndGetOracleSequence(dataSource, PRODUCER_RECORD_ID_SEQ);

        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            statement.setLong(1, id);
            statement.setString(2, record.topic());
            statement.setBytes(3, keySerializer.serialize(record.topic(), record.key()));
            statement.setBytes(4, valueSerializer.serialize(record.topic(), record.value()));
            statement.executeUpdate();
        }

        return id;
    }

    @SneakyThrows
    @Override
    public void deleteRecord(long id) {
        String sql = format("DELETE FROM %s WHERE %s = ?", PRODUCER_RECORD_TABLE, ID);
        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public List<KafkaProducerRecord<K, V>> getRecords(List<String> topics, Instant olderThan, int maxMessages) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ANY(?) AND %s >= ? FETCH NEXT %d ROWS ONLY",
                PRODUCER_RECORD_TABLE, TOPIC, CREATED_AT, maxMessages
        );

        try(PreparedStatement statement = createPreparedStatement(dataSource, sql)) {
            Array array = dataSource.getConnection().createArrayOf("VARCHAR", topics.toArray());
            statement.setArray(1, array);
            statement.setTimestamp(2, new Timestamp(olderThan.toEpochMilli()));
            return fetchProducerRecords(statement.executeQuery(), keyDeserializer, valueDeserializer);
        }
    }

}
