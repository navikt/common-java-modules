package no.nav.common.kafka.utils;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.SneakyThrows;

public class DbUtils {

    @SneakyThrows
    public static void runScript(DataSource dataSource, String resourceFile) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String sql = TestUtils.readTestResourceFile(resourceFile);
            statement.execute(sql);
        }
    }

    @SneakyThrows
    public static void cleanupConsumer(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_CONSUMER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_CONSUMER_RECORD");
        }
    }

    @SneakyThrows
    public static void cleanupProducer(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_PRODUCER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_PRODUCER_RECORD");
        }
    }
}
