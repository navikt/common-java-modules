package no.nav.common.kafka.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class LocalPostgresDatabaseTest {

    @Test
    public void bruker_containerens_tilkoblingsinnstillinger_uten_aa_starte_poolen() {
        // Arrange
        var container = mock(PostgreSQLContainer.class);
        when(container.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost:5432/test");
        when(container.getUsername()).thenReturn("test");
        when(container.getPassword()).thenReturn("test-password");

        // Act
        HikariDataSource dataSource = LocalPostgresDatabase.createPostgresDataSource(container);
        try (dataSource) {
            // Assert
            assertEquals(container.getJdbcUrl(), dataSource.getJdbcUrl());
            assertEquals(container.getUsername(), dataSource.getUsername());
            assertEquals(container.getPassword(), dataSource.getPassword());
            assertNull(dataSource.getHikariPoolMXBean());
        }
        assertTrue(dataSource.isClosed());
    }

    @Test
    public void kan_aapne_tilkobling_og_lukke_poolen() throws Exception {
        // Arrange
        var container = mock(PostgreSQLContainer.class);
        when(container.getJdbcUrl()).thenReturn("jdbc:h2:mem:hikari-test");
        when(container.getUsername()).thenReturn("sa");
        when(container.getPassword()).thenReturn("");

        // Act
        HikariDataSource dataSource = LocalPostgresDatabase.createPostgresDataSource(container);
        try (dataSource;
                Connection connection = dataSource.getConnection()) {
            // Assert
            assertTrue(connection.isValid(1));
        }
        assertTrue(dataSource.isClosed());
    }
}
