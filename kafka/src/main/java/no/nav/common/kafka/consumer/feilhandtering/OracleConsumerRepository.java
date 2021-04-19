package no.nav.common.kafka.consumer.feilhandtering;

import lombok.SneakyThrows;
import org.apache.kafka.common.TopicPartition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.*;

public class OracleConsumerRepository implements KafkaConsumerRepository {

    private final DataSource dataSource;

    private final String consumerRecordTable;

    public OracleConsumerRepository(DataSource dataSource, String consumerRecordTable) {
        this.dataSource = dataSource;
        this.consumerRecordTable = consumerRecordTable;
    }

    public OracleConsumerRepository(DataSource dataSource) {
        this(dataSource, CONSUMER_RECORD_TABLE);
    }

    @SneakyThrows
    @Override
    public long storeRecord(StoredConsumerRecord record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                consumerRecordTable, ID, TOPIC, PARTITION, RECORD_OFFSET, KEY, VALUE, HEADERS_JSON, RECORD_TIMESTAMP
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            long id = incrementAndGetOracleSequence(connection, CONSUMER_RECORD_ID_SEQ);

            statement.setLong(1, id);
            statement.setString(2, record.getTopic());
            statement.setInt(3, record.getPartition());
            statement.setLong(4, record.getOffset());
            statement.setBytes(5, record.getKey());
            statement.setBytes(6, record.getValue());
            statement.setString(7, record.getHeadersJson());
            statement.setLong(8, record.getTimestamp());
            statement.executeUpdate();

            return id;
        } catch (SQLIntegrityConstraintViolationException e) {
            return -1;
        }
    }

    @SneakyThrows
    @Override
    public void deleteRecords(List<Long> ids) {
        String sql = format("DELETE FROM %s WHERE %s " + inClause(ids.size()), consumerRecordTable, ID);

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < ids.size(); i++) {
                statement.setLong(i + 1, ids.get(i));
            }
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public boolean hasRecordWithKey(String topic, int partition, byte[] key) {
        String sql = format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? AND %s = ? FETCH NEXT 1 ROWS ONLY",
                ID, consumerRecordTable, TOPIC, PARTITION, KEY
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, topic);
            statement.setInt(2, partition);
            statement.setBytes(3, key);

            return statement.executeQuery().next();
        }
    }

    @SneakyThrows
    @Override
    public List<StoredConsumerRecord> getRecords(String topic, int partition, int maxRecords) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ? AND %s = ? ORDER BY %s FETCH NEXT %d ROWS ONLY",
                consumerRecordTable, TOPIC, PARTITION, RECORD_OFFSET, maxRecords
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, topic);
            statement.setInt(2, partition);
            return fetchConsumerRecords(statement.executeQuery());
        }
    }

    @SneakyThrows
    @Override
    public void incrementRetries(long id) {
        String sql = format(
                "UPDATE %s SET %s = %s + 1, %s = CURRENT_TIMESTAMP WHERE %s = ?",
                consumerRecordTable, RETRIES, RETRIES, LAST_RETRY, ID
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            statement.execute();
        }
    }

    @SneakyThrows
    @Override
    public List<TopicPartition> getTopicPartitions(List<String> topics) {
        String sql = format(
                "SELECT DISTINCT %s, %s FROM %s WHERE %s " + inClause(topics.size()),
                TOPIC, PARTITION, consumerRecordTable, TOPIC
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < topics.size(); i++) {
                statement.setString(i + 1, topics.get(i));
            }
            return fetchTopicPartitions(statement.executeQuery());
        }
    }
}
