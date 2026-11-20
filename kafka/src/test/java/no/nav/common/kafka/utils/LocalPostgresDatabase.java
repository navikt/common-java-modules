package no.nav.common.kafka.utils;

import javax.sql.DataSource;
import org.postgresql.ds.PGPoolingDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

public class LocalPostgresDatabase {

    public static PostgreSQLContainer<?> createPostgresContainer() {
        return new PostgreSQLContainer<>("postgres:12-alpine").waitingFor(new HostPortWaitStrategy());
    }

    public static DataSource createPostgresDataSource(PostgreSQLContainer<?> container) {
        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setURL(container.getJdbcUrl());
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        return dataSource;
    }
}
