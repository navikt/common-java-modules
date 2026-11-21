package no.nav.common.kafka.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class LocalPostgresDatabase {

    public static PostgreSQLContainer createPostgresTestContainer() {
        // Kalleren eier containeren og må stoppe den etter bruk.
        //noinspection resource
        return new PostgreSQLContainer("postgres:17-alpine").waitingFor(new HostPortWaitStrategy());
    }

    public static HikariDataSource createPostgresDataSource(JdbcDatabaseContainer<?> container) {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(container.getJdbcUrl());
        dataSource.setUsername(container.getUsername());
        dataSource.setPassword(container.getPassword());
        return dataSource;
    }
}
