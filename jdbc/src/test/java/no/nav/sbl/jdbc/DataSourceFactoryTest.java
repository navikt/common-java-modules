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

    private static final String URL_VALUE = "jdbc:hsqldb:mem:" + DataSourceFactoryTest.class.getSimpleName();
    private static final String USERNAME_VALUE = "root";
    private static final String PASSWORD_VALUE = "1234";
    private static final String URL = "TEST_DB_URL";
    private static final String USERNAME = "TEST_DB_USERNAME";
    private static final String PASSWORD = "TEST_DB_PASSWORD";

    @BeforeClass
    public static void initializeEnvironment() {
        System.setProperty(URL, URL_VALUE);
        System.setProperty(USERNAME, USERNAME_VALUE);
        System.setProperty(PASSWORD, PASSWORD_VALUE);
    }

    @Before
    public void initializeDataSource() {
        ds = DataSourceFactory.dataSource()
                .url(System.getProperty(URL))
                .username(System.getProperty(USERNAME))
                .password(System.getProperty(PASSWORD))
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
        assertEquals(URL_VALUE, ds.getJdbcUrl());
        assertEquals(USERNAME_VALUE, ds.getUsername());
        assertEquals(PASSWORD_VALUE, ds.getPassword());
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
                .username(System.getProperty(USERNAME))
                .password(System.getProperty(PASSWORD))
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testUsernameNotSet() {
        ds = DataSourceFactory.dataSource()
                .url(System.getProperty(URL))
                .password(System.getProperty(PASSWORD))
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testPasswordNotSet() {
        ds = DataSourceFactory.dataSource()
                .url(System.getProperty(URL))
                .username(System.getProperty(USERNAME))
                .maxPoolSize(300)
                .minimumIdle(1)
                .build();
    }
}
