package no.nav.sbl.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import no.nav.sbl.dialogarena.types.Pingable;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseSelftest implements Pingable {
    private final HikariDataSource dataSource;

    public DatabaseSelftest(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata("db", dataSource.getJdbcUrl(), "database", true);
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1")) {
                    resultSet.next();
                    int anInt = resultSet.getInt(1);
                    if (anInt != 1) {
                        throw new IllegalStateException();
                    }
                }
            }
            return Ping.lyktes(metadata);
        } catch (Throwable t) {
            return Ping.feilet(metadata, t);
        }
    }

}
