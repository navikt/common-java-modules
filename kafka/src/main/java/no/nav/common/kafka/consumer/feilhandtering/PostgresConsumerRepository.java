package no.nav.common.kafka.consumer.feilhandtering;

import lombok.SneakyThrows;
import org.apache.kafka.common.TopicPartition;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.*;

public class PostgresConsumerRepository implements KafkaConsumerRepository {

    private final static String UNIQUE_VIOLATION_ERROR_CODE = "23505";

    private final DataSource dataSource;

    private final String consumerRecordTable;

    public PostgresConsumerRepository(DataSource dataSource, String consumerRecordTable) {
        this.dataSource = dataSource;
        this.consumerRecordTable = consumerRecordTable;
    }

    public PostgresConsumerRepository(DataSource dataSource) {
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
            long id = incrementAndGetPostgresSequence(connection, CONSUMER_RECORD_ID_SEQ);

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
        } catch (SQLException e) {
            // The Postgres driver does not throw SQLIntegrityConstraintViolationException (but might at a later date),
            //  therefore we need to check the error code as well
            if (e instanceof SQLIntegrityConstraintViolationException || UNIQUE_VIOLATION_ERROR_CODE.equals(e.getSQLState())) {
                return -1;
            }

            throw e;
        }
    }

    @SneakyThrows
    @Override
    public void deleteRecords(List<Long> ids) {
        String sql = format("DELETE FROM %s WHERE %s = ANY(?)", consumerRecordTable, ID);

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            Array array = connection.createArrayOf("INTEGER", ids.toArray());
            statement.setArray(1, array);
            statement.executeUpdate();
        }
    }

    @SneakyThrows
    @Override
    public boolean hasRecordWithKey(String topic, int partition, byte[] key) {
        String sql = format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? AND %s = ? LIMIT 1",
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
                "SELECT * FROM %s WHERE %s = ? AND %s = ? ORDER BY %s LIMIT %d",
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
                "SELECT DISTINCT %s, %s FROM %s WHERE %s = ANY(?)",
                TOPIC, PARTITION, consumerRecordTable, TOPIC
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            Array array = connection.createArrayOf("VARCHAR", topics.toArray());
            statement.setArray(1, array);
            return fetchTopicPartitions(statement.executeQuery());
        }
    }

}
