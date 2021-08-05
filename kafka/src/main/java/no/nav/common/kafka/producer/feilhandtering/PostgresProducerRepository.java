package no.nav.common.kafka.producer.feilhandtering;

import lombok.SneakyThrows;
import no.nav.common.kafka.util.DatabaseUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.incrementAndGetPostgresSequence;
import static no.nav.common.kafka.util.DatabaseUtils.toPostgresArray;

public class PostgresProducerRepository implements KafkaProducerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final String producerRecordTable;

    public PostgresProducerRepository(JdbcTemplate jdbcTemplate, String producerRecordTableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.producerRecordTable = producerRecordTableName;
    }

    public PostgresProducerRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, PRODUCER_RECORD_TABLE);
    }

    @SneakyThrows
    @Override
    public long storeRecord(StoredProducerRecord record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
                producerRecordTable, ID, TOPIC, KEY, VALUE, HEADERS_JSON
        );

        long id = incrementAndGetPostgresSequence(jdbcTemplate, PRODUCER_RECORD_ID_SEQ);

        jdbcTemplate.update(
                sql,
                id,
                record.getTopic(),
                record.getKey(),
                record.getValue(),
                record.getHeadersJson());

        return id;
    }

    @SneakyThrows
    @Override
    public void deleteRecords(List<Long> ids) {
        String sql = format("DELETE FROM %s WHERE %s = ANY(?::bigint[])", producerRecordTable, ID);

        jdbcTemplate.update(sql, toPostgresArray(ids));
    }

    @SneakyThrows
    @Override
    public List<StoredProducerRecord> getRecords(int maxMessages) {
        String sql = format(
                "SELECT * FROM %s ORDER BY %s LIMIT %d",
                producerRecordTable, ID, maxMessages
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchProducerRecords);
    }

    @SneakyThrows
    @Override
    public List<StoredProducerRecord> getRecords(int maxMessages, List<String> topics) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ANY(?::varchar[]) ORDER BY %s LIMIT %d",
                producerRecordTable, TOPIC, ID, maxMessages
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchProducerRecords, toPostgresArray(topics));
    }

}
