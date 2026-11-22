package no.nav.common.kafka.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public class DbUtils {

    public static void runScript(DataSource dataSource, String resourceFile)
            throws SQLException, URISyntaxException, IOException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String sql = TestUtils.readTestResourceFile(resourceFile);
            statement.execute(sql);
        }
    }

    public static void cleanupConsumer(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_CONSUMER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_CONSUMER_RECORD");
        }
    }

    public static void cleanupProducer(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_PRODUCER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_PRODUCER_RECORD");
        }
    }
}
