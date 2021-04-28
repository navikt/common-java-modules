package no.nav.common.kafka.utils;

import org.postgresql.ds.PGPoolingDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

public class LocalPostgresDatabase {

    public static PostgreSQLContainer<?> createPostgresContainer() {
        return new PostgreSQLContainer<>("postgres:12-alpine");
    }

    public static DataSource createPostgresDataSource(PostgreSQLContainer<?> container) {
        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setURL(container.getJdbcUrl());
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        return dataSource;
    }

}
