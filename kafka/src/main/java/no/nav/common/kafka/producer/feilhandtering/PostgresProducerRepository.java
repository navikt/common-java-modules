package no.nav.common.kafka.producer.feilhandtering;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.fetchProducerRecords;
import static no.nav.common.kafka.util.DatabaseUtils.incrementAndGetPostgresSequence;

public class PostgresProducerRepository implements KafkaProducerRepository {

    private final DataSource dataSource;

    private final String producerRecordTable;

    public PostgresProducerRepository(DataSource dataSource, String producerRecordTableName) {
        this.dataSource = dataSource;
        this.producerRecordTable = producerRecordTableName;
    }

    public PostgresProducerRepository(DataSource dataSource) {
        this(dataSource, PRODUCER_RECORD_TABLE);
    }

    @SneakyThrows
    @Override
    public long storeRecord(StoredProducerRecord record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
                producerRecordTable, ID, TOPIC, KEY, VALUE, HEADERS_JSON
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            long id = incrementAndGetPostgresSequence(connection, PRODUCER_RECORD_ID_SEQ);

            statement.setLong(1, id);
            statement.setString(2, record.getTopic());
            statement.setBytes(3, record.getKey());
            statement.setBytes(4, record.getValue());
            statement.setString(5, record.getHeadersJson());
            statement.executeUpdate();

            return id;
        }
    }

    @SneakyThrows
    @Override
    public void deleteRecords(List<Long> ids) {
        String sql = format("DELETE FROM %s WHERE %s = ANY(?)", producerRecordTable, ID);

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
    public List<StoredProducerRecord> getRecords(int maxMessages) {
        String sql = format(
                "SELECT * FROM %s ORDER BY %s LIMIT %d",
                producerRecordTable, ID, maxMessages
        );

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            return fetchProducerRecords(statement.executeQuery());
        }
    }

}
