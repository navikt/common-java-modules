package no.nav.sbl.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class DataSourceFactoryTest {

    private HikariDataSource ds;

    private static final String URL = "jdbc:hsqldb:mem:" + DataSourceFactoryTest.class.getSimpleName();
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1234";

    @Before
    public void initializeDataSource() {
        ds = DataSourceFactory.dataSource()
                .url(URL)
                .username(USERNAME)
                .password(PASSWORD)
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }

    @After
    public void cleanup() {
        ds.close();
    }

    @Test
    public void testDataSourceConfiguration() {
        assertEquals(300, ds.getMaximumPoolSize());
        assertEquals(1, ds.getMinimumIdle());
        assertEquals(URL, ds.getJdbcUrl());
        assertEquals(USERNAME, ds.getUsername());
        assertEquals(PASSWORD, ds.getPassword());
    }

    @Test
    public void testConnection() {
        try {
            ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUrlNotSet() {
        ds = DataSourceFactory.dataSource()
                .username(USERNAME)
                .password(PASSWORD)
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testUsernameNotSet() {
        ds = DataSourceFactory.dataSource()
                .url(URL)
                .password(PASSWORD)
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testPasswordNotSet() {
        ds = DataSourceFactory.dataSource()
                .url(URL)
                .username(USERNAME)
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }
}
