package no.nav.sbl.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;

public class DataSourceFactory {

    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 300;
    private static final int DEFAULT_MINIMUM_IDLE = 1;

    private DataSourceFactory() { }

    public static Builder dataSource() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String username;
        private String password;
        private int maxPoolSize;
        private int minimumIdle;

        public Builder() {
            this.maxPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
            this.minimumIdle = DEFAULT_MINIMUM_IDLE;
        }

        /* Required */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /* Required */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /* Required */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder maxPoolSize(int poolSize) {
            this.maxPoolSize = poolSize;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public HikariDataSource build() {
            checkStateValidity();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(this.url);
            config.setUsername(this.username);
            config.setPassword(this.password);
            config.setMaximumPoolSize(this.maxPoolSize);
            config.setMinimumIdle(this.minimumIdle);
            config.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());

            return new HikariDataSource(config);
        }

        private void checkStateValidity() {
            if (this.url == null) {
                throw new IllegalStateException("url ikke definert før kall på build()");
            }

            if (this.username == null) {
                throw new IllegalStateException("username ikke definert før kall på build()");
            }

            if (this.password == null) {
                throw new IllegalStateException("password ikke definert før kall på build()");
            }
        }
    }
}
