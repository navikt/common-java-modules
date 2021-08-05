package no.nav.common.kafka.spring;

import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;
import static no.nav.common.kafka.spring.DatabaseUtils.incrementAndGetPostgresSequence;
import static no.nav.common.kafka.spring.DatabaseUtils.toPostgresArray;

public class PostgresJdbcTemplateConsumerRepository implements KafkaConsumerRepository {

    private final JdbcTemplate jdbcTemplate;

    private final String consumerRecordTable;

    public PostgresJdbcTemplateConsumerRepository(JdbcTemplate jdbcTemplate, String consumerRecordTable) {
        this.jdbcTemplate = jdbcTemplate;
        this.consumerRecordTable = consumerRecordTable;
    }

    public PostgresJdbcTemplateConsumerRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, CONSUMER_RECORD_TABLE);
    }

    @SneakyThrows
    @Override
    public long storeRecord(StoredConsumerRecord record) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                consumerRecordTable, ID, TOPIC, PARTITION, RECORD_OFFSET, KEY, VALUE, HEADERS_JSON, RECORD_TIMESTAMP
        );
        long id = incrementAndGetPostgresSequence(jdbcTemplate, CONSUMER_RECORD_ID_SEQ);

        try {
            jdbcTemplate.update(
                    sql,
                    id,
                    record.getTopic(),
                    record.getPartition(),
                    record.getOffset(),
                    record.getKey(),
                    record.getValue(),
                    record.getHeadersJson(),
                    record.getTimestamp());

            return id;
        } catch (DuplicateKeyException e) {
            return -1;
        }
    }

    @SneakyThrows
    @Override
    public void deleteRecords(List<Long> ids) {
        String sql = format("DELETE FROM %s WHERE %s = ANY(?::bigint[])", consumerRecordTable, ID);

        jdbcTemplate.update(sql, toPostgresArray(ids));
    }

    @SneakyThrows
    @Override
    public boolean hasRecordWithKey(String topic, int partition, byte[] key) {
        String sql = format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? AND %s = ? LIMIT 1",
                ID, consumerRecordTable, TOPIC, PARTITION, KEY
        );

        return jdbcTemplate.query(sql, ResultSet::next, topic, partition, key);
    }

    @SneakyThrows
    @Override
    public List<StoredConsumerRecord> getRecords(String topic, int partition, int maxRecords) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ? AND %s = ? ORDER BY %s LIMIT %d",
                consumerRecordTable, TOPIC, PARTITION, RECORD_OFFSET, maxRecords
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchConsumerRecords, topic, partition);
    }

    @SneakyThrows
    @Override
    public void incrementRetries(long id) {
        String sql = format(
                "UPDATE %s SET %s = %s + 1, %s = CURRENT_TIMESTAMP WHERE %s = ?",
                consumerRecordTable, RETRIES, RETRIES, LAST_RETRY, ID
        );

        jdbcTemplate.update(sql, id);
    }

    @SneakyThrows
    @Override
    public List<TopicPartition> getTopicPartitions(List<String> topics) {
        String sql = format(
                "SELECT DISTINCT %s, %s FROM %s WHERE %s = ANY(?::varchar[])",
                TOPIC, PARTITION, consumerRecordTable, TOPIC
        );

        return jdbcTemplate.query(sql, DatabaseUtils::fetchTopicPartitions, toPostgresArray(topics));
    }

}
