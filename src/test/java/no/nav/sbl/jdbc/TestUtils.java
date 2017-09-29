package no.nav.sbl.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestUtils {

    private static int counter;

    public static JdbcTemplate jdbcTemplate() {
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        String url = "jdbc:h2:mem:" + TestUtils.class.getSimpleName() + (counter++) + ";MODE=Oracle;DB_CLOSE_DELAY=-1";
        jdbcDataSource.setUrl(url);
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("");
        System.out.println(url);
        return new JdbcTemplate(jdbcDataSource);
    }

}
