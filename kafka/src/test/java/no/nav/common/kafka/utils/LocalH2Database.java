package no.nav.common.kafka.utils;

import lombok.SneakyThrows;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalH2Database {

    private final static AtomicInteger counter = new AtomicInteger();

    public enum DatabaseType {
        POSTGRES, ORACLE
    }

    public static DataSource createDatabase(DatabaseType type) {
        String dbType = type == DatabaseType.ORACLE ? "Oracle" : "PostgreSQL";

        String url = String.format("jdbc:h2:mem:common-db-%d;DB_CLOSE_DELAY=-1;MODE=%s;BUILTIN_ALIAS_OVERRIDE=1;", counter.incrementAndGet(), dbType);

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(url);

        return dataSource;
    }

    @SneakyThrows
    public static void runScript(DataSource dataSource, String resourceFile) {
        try(Statement statement = dataSource.getConnection().createStatement()) {
            String sql = TestUtils.readTestResourceFile(resourceFile);
            statement.execute(sql);
        }
    }

    @SneakyThrows
    public static void cleanupConsumer(DataSource dataSource) {
        try(Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_CONSUMER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_CONSUMER_RECORD");
        }
    }

    @SneakyThrows
    public static void cleanupProducer(DataSource dataSource) {
        try(Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("ALTER SEQUENCE KAFKA_PRODUCER_RECORD_ID_SEQ RESTART WITH 1");
            statement.execute("DELETE FROM KAFKA_PRODUCER_RECORD");
        }
    }

}
