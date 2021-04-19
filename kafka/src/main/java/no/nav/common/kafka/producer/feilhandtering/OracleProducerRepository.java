package no.nav.common.kafka.producer.feilhandtering;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.*;

public class OracleProducerRepository implements KafkaProducerRepository {

    private final DataSource dataSource;

    private final String producerRecordTable;

    public OracleProducerRepository(DataSource dataSource, String producerRecordTableName) {
        this.dataSource = dataSource;
        this.producerRecordTable = producerRecordTableName;
    }

    public OracleProducerRepository(DataSource dataSource) {
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
            long id = incrementAndGetOracleSequence(connection, PRODUCER_RECORD_ID_SEQ);

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
        String sql = format("DELETE FROM %s WHERE %s " + inClause(ids.size()), producerRecordTable, ID);

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
    public List<StoredProducerRecord> getRecords(int maxMessages) {
        String sql = format(
                "SELECT * FROM %s ORDER BY %s FETCH NEXT %d ROWS ONLY",
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
