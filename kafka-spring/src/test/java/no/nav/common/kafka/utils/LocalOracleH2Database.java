package no.nav.common.kafka.utils;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalOracleH2Database {

    private final static AtomicInteger counter = new AtomicInteger();

    public static DataSource createDatabase() {
        String url = String.format("jdbc:h2:mem:common-db-%d;DB_CLOSE_DELAY=-1;MODE=Oracle;BUILTIN_ALIAS_OVERRIDE=1;NON_KEYWORDS=PARTITION,KEY,VALUE", counter.incrementAndGet());

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(url);

        return dataSource;
    }

}
