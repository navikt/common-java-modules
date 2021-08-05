package no.nav.common.kafka.producer.feilhandtering;

import lombok.SneakyThrows;
import no.nav.common.kafka.util.DatabaseUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.util.DatabaseUtils.inClause;
import static no.nav.common.kafka.util.DatabaseUtils.incrementAndGetOracleSequence;

public class OracleProducerRepository implements KafkaProducerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final String producerRecordTable;

    public OracleProducerRepository(JdbcTemplate jdbcTemplate, String producerRecordTableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.producerRecordTable = producerRecordTableName;
    }

    public OracleProducerRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, PRODUCER_RECORD_TABLE);
    }

    @SneakyThrows
    @Override
    public long storeRecord(StoredProducerRecord record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
                producerRecordTable, ID, TOPIC, KEY, VALUE, HEADERS_JSON
        );

        long id = incrementAndGetOracleSequence(jdbcTemplate, PRODUCER_RECORD_ID_SEQ);

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
        String sql = format("DELETE FROM %s WHERE %s " + inClause(ids.size()), producerRecordTable, ID);

        jdbcTemplate.update(sql, ids.toArray());
    }

    @SneakyThrows
    @Override
    public List<StoredProducerRecord> getRecords(int maxMessages) {
        String sql = format(
                "SELECT * FROM %s ORDER BY %s FETCH NEXT %d ROWS ONLY",
                producerRecordTable, ID, maxMessages
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchProducerRecords);
    }

    @SneakyThrows
    @Override
    public List<StoredProducerRecord> getRecords(int maxMessages, List<String> topics) {
        String sql = format(
                "SELECT * FROM %s WHERE %s " + inClause(topics.size()) + " ORDER BY %s FETCH NEXT %d ROWS ONLY",
                producerRecordTable, TOPIC, ID, maxMessages
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchProducerRecords, topics.toArray());
    }

}
