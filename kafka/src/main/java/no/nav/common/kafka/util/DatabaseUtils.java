package no.nav.common.kafka.util;

import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecord;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecord;
import org.apache.kafka.common.TopicPartition;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static no.nav.common.kafka.util.DatabaseConstants.*;

public class DatabaseUtils {

    @SneakyThrows
    public static PreparedStatement createPreparedStatement(DataSource dataSource, String sql) {
        return dataSource.getConnection().prepareStatement(sql);
    }

    @SneakyThrows
    public static List<KafkaProducerRecord> fetchProducerRecords(ResultSet resultSet) {
        List<KafkaProducerRecord> records = new ArrayList<>();

        while (resultSet.next()) {
            long id = resultSet.getInt(ID);
            String topic = resultSet.getString(TOPIC);
            String headersJson = resultSet.getString(HEADERS_JSON);
            byte[] key = resultSet.getBytes(KEY);
            byte[] value = resultSet.getBytes(VALUE);

            records.add(new KafkaProducerRecord(id, topic, key, value, headersJson));
        }

        return records;
    }

    @SneakyThrows
    public static List<KafkaConsumerRecord> fetchConsumerRecords(ResultSet resultSet) {
        List<KafkaConsumerRecord> records = new ArrayList<>();

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

            records.add(new KafkaConsumerRecord(id, topic, partition, offset, key, value, headersJson, retries, lastRetry));
        }

        return records;
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
    public static long incrementAndGetPostgresSequence(DataSource dataSource, String sequenceName) {
        String sql = format("SELECT nextval('%s')", sequenceName);

        try(Statement statement = dataSource.getConnection().createStatement()) {
            return fetchSequence(statement.executeQuery(sql));
        }
    }

    @SneakyThrows
    public static long incrementAndGetOracleSequence(DataSource dataSource, String sequenceName) {
        String sql = format("SELECT %s.NEXTVAL FROM dual", sequenceName);

        try(Statement statement = dataSource.getConnection().createStatement()) {
            return fetchSequence(statement.executeQuery(sql));
        }
    }

    @SneakyThrows
    public static long fetchSequence(ResultSet resultSet) {
        if (!resultSet.next()) {
            throw new IllegalStateException("Result set does not contain sequence");
        }

        return resultSet.getLong(1);
    }

}
