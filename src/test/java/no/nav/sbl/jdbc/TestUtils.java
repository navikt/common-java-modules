package no.nav.sbl.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestUtils {

    private static int counter;

    public static JdbcTemplate jdbcTemplate() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        String url = "jdbc:hsqldb:mem:" + TestUtils.class.getSimpleName() + (counter++);
        dataSource.setSuppressClose(true);
        dataSource.setUrl(url);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        System.out.println(url);
        setHsqlToOraSyntax(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static void setHsqlToOraSyntax(SingleConnectionDataSource ds) {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
