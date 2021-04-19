package no.nav.common.kafka.util;

import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord;
import org.apache.kafka.common.TopicPartition;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;

public class DatabaseUtils {

    @SneakyThrows
    public static List<StoredProducerRecord> fetchProducerRecords(ResultSet resultSet) {
        try (resultSet) {
            List<StoredProducerRecord> records = new ArrayList<>();

            while (resultSet.next()) {
                long id = resultSet.getInt(ID);
                String topic = resultSet.getString(TOPIC);
                String headersJson = resultSet.getString(HEADERS_JSON);
                byte[] key = resultSet.getBytes(KEY);
                byte[] value = resultSet.getBytes(VALUE);

                records.add(new StoredProducerRecord(id, topic, key, value, headersJson));
            }

            return records;
        }
    }

    @SneakyThrows
    public static List<StoredConsumerRecord> fetchConsumerRecords(ResultSet resultSet) {
        try (resultSet) {
            List<StoredConsumerRecord> records = new ArrayList<>();

            while (resultSet.next()) {
                long id = resultSet.getInt(ID);
                String topic = resultSet.getString(TOPIC);
                int partition = resultSet.getInt(PARTITION);
                long offset = resultSet.getLong(RECORD_OFFSET);
                byte[] key = resultSet.getBytes(KEY);
                byte[] value = resultSet.getBytes(VALUE);
                String headersJson = resultSet.getString(HEADERS_JSON);
                int retries = resultSet.getInt(RETRIES);
                Timestamp lastRetry = resultSet.getTimestamp(LAST_RETRY);
                long timestamp = resultSet.getLong(RECORD_TIMESTAMP);

                records.add(new StoredConsumerRecord(id, topic, partition, offset, key, value, headersJson, retries, lastRetry, timestamp));
            }

            return records;
        }
    }

    @SneakyThrows
    public static List<TopicPartition> fetchTopicPartitions(ResultSet resultSet) {
        List<TopicPartition> topicPartitions = new ArrayList<>();

        while (resultSet.next()) {
            String topic = resultSet.getString(TOPIC);
            int partition = resultSet.getInt(PARTITION);

            topicPartitions.add(new TopicPartition(topic, partition));
        }

        return topicPartitions;
    }

    @SneakyThrows
    public static long incrementAndGetPostgresSequence(Connection connection, String sequenceName) {
        String sql = format("SELECT nextval('%s')", sequenceName);

        try(Statement statement = connection.createStatement()) {
            return fetchSequence(statement.executeQuery(sql));
        }
    }

    @SneakyThrows
    public static long incrementAndGetOracleSequence(Connection connection, String sequenceName) {
        String sql = format("SELECT %s.NEXTVAL FROM dual", sequenceName);

        try(Statement statement = connection.createStatement()) {
            return fetchSequence(statement.executeQuery(sql));
        }
    }

    @SneakyThrows
    public static long fetchSequence(ResultSet resultSet) {
        try (resultSet) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Result set does not contain sequence");
            }

            return resultSet.getLong(1);
        }
    }

    public static String inClause(int number) {
        StringBuilder sb = new StringBuilder();
        sb.append("IN (");
        for (int i = 0; i < number; i++ ) {
            sb.append("?");
            if (i < number - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
