package no.nav.common.kafka.utils;

import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

public class LocalOracleH2Database {

    private static final AtomicInteger counter = new AtomicInteger();

    public static DataSource createDatabase() {
        String url = String.format(
                "jdbc:h2:mem:common-db-%d;DB_CLOSE_DELAY=-1;MODE=Oracle;BUILTIN_ALIAS_OVERRIDE=1;NON_KEYWORDS=PARTITION,KEY,VALUE",
                counter.incrementAndGet());

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(url);

        return dataSource;
    }
}
