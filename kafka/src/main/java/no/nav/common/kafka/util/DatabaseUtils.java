package no.nav.common.kafka.util;

import lombok.SneakyThrows;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecord;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;

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
    public static <K, V> List<KafkaProducerRecord<K, V>> fetchProducerRecords(
            ResultSet resultSet,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {

        List<KafkaProducerRecord<K, V>> records = new ArrayList<>();

        while (resultSet.next()) {
            long id = resultSet.getInt(ID);
            String topic = resultSet.getString(TOPIC);
            K key = keyDeserializer.deserialize(topic, resultSet.getBytes(KEY));
            V value = valueDeserializer.deserialize(topic, resultSet.getBytes(VALUE));

            records.add(new KafkaProducerRecord<>(id, topic, key, value));
        }

        return records;
    }

    @SneakyThrows
    public static <K, V> List<KafkaConsumerRecord<K, V>> fetchConsumerRecords(
            ResultSet resultSet,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    ) {

        List<KafkaConsumerRecord<K, V>> records = new ArrayList<>();

        while (resultSet.next()) {
            long id = resultSet.getInt(ID);
            String topic = resultSet.getString(TOPIC);
            int partition = resultSet.getInt(PARTITION);
            long offset = resultSet.getLong(RECORD_OFFSET);
            int retries = resultSet.getInt(RETRIES);
            Timestamp lastRetry = resultSet.getTimestamp(LAST_RETRY);

            K key = keyDeserializer.deserialize(topic, resultSet.getBytes(KEY));
            V value = valueDeserializer.deserialize(topic, resultSet.getBytes(VALUE));

            records.add(new KafkaConsumerRecord<>(id, topic, partition, offset, key, value, retries, lastRetry));
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
