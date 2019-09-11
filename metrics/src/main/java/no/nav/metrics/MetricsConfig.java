package no.nav.metrics;

import no.nav.util.sbl.EnvironmentUtils;

import java.net.UnknownHostException;

import static no.nav.util.sbl.EnvironmentUtils.getOptionalProperty;
import static no.nav.util.sbl.EnvironmentUtils.requireNamespace;

public final class MetricsConfig {

    public static final String SENSU_CLIENT_HOST = "sensu_client_host";
    public static final String SENSU_CLIENT_PORT = "sensu_client_port";

    public static final String SENSU_RETRY_INTERVAL_PROPERTY_NAME = "metrics.sensu.report.retryInterval";
    public static final String SENSU_QUEUE_SIZE_PROPERTY_NAME = "metrics.sensu.report.queueSize";
    public static final String SENSU_BATCHES_PER_SECOND_PROPERTY_NAME = "metrics.sensu.report.batchesPerSecond";
    public static final String SENSU_BATCH_SIZE_PROPERTY_NAME = "metrics.sensu.report.batchSize";

    private final String sensuHost;
    private final int sensuPort;

    private final String application;
    private final String hostname;
    private final String environment;

    private final int retryInterval;
    private final int queueSize;
    private final int batchesPerSecond;
    private final int batchSize;

    MetricsConfig(String sensuHost, int sensuPort, String application, String hostname, String environment, int retryInterval, int queueSize, int batchesPerSecond, int batchSize) {
        this.sensuHost = sensuHost;
        this.sensuPort = sensuPort;
        this.application = application;
        this.hostname = hostname;
        this.environment = environment;
        this.retryInterval = retryInterval;
        this.queueSize = queueSize;
        this.batchesPerSecond = batchesPerSecond;
        this.batchSize = batchSize;
    }

    public static MetricsConfig resolveSkyaConfig() throws UnknownHostException {
        return defaultConfig("localhost", 3030);
    }

    public static MetricsConfig resolveNaisConfig() throws UnknownHostException {
        return defaultConfig("sensu.nais", 3030);
    }

    private static MetricsConfig defaultConfig(String host, int port) throws UnknownHostException {
        return withSensuDefaults(MetricsConfig.builder()
                .sensuHost(getOptionalProperty(SENSU_CLIENT_HOST).orElse(host))
                .sensuPort(getOptionalProperty(SENSU_CLIENT_PORT).map(Integer::parseInt).orElse(port))

                .application(EnvironmentUtils.requireApplicationName())
                .environment(EnvironmentUtils.getEnvironmentName().orElse(requireNamespace()))
                .hostname(EnvironmentUtils.resolveHostName())

                .build()
        );
    }

    public static MetricsConfig withSensuDefaults(MetricsConfig metricsConfig) {
        return metricsConfig
                .withRetryInterval(defaultIntSystemProperty(SENSU_RETRY_INTERVAL_PROPERTY_NAME, 1000))
                .withQueueSize(defaultIntSystemProperty(SENSU_QUEUE_SIZE_PROPERTY_NAME, 20_000))
                .withBatchesPerSecond(defaultIntSystemProperty(SENSU_BATCHES_PER_SECOND_PROPERTY_NAME, 50))
                .withBatchSize(defaultIntSystemProperty(SENSU_BATCH_SIZE_PROPERTY_NAME, 100));
    }

    private static int defaultIntSystemProperty(String propertyName, int defaultValue) {
        return Integer.parseInt(System.getProperty(propertyName, Integer.toString(defaultValue)));
    }

    public static MetricsConfigBuilder builder() {
        return new MetricsConfigBuilder();
    }

    public String getSensuHost() {
        return this.sensuHost;
    }

    public int getSensuPort() {
        return this.sensuPort;
    }

    public String getApplication() {
        return this.application;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getEnvironment() {
        return this.environment;
    }

    public int getRetryInterval() {
        return this.retryInterval;
    }

    public int getQueueSize() {
        return this.queueSize;
    }

    public int getBatchesPerSecond() {
        return this.batchesPerSecond;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MetricsConfig)) return false;
        final MetricsConfig other = (MetricsConfig) o;
        final Object this$sensuHost = this.getSensuHost();
        final Object other$sensuHost = other.getSensuHost();
        if (this$sensuHost == null ? other$sensuHost != null : !this$sensuHost.equals(other$sensuHost)) return false;
        if (this.getSensuPort() != other.getSensuPort()) return false;
        final Object this$application = this.getApplication();
        final Object other$application = other.getApplication();
        if (this$application == null ? other$application != null : !this$application.equals(other$application))
            return false;
        final Object this$hostname = this.getHostname();
        final Object other$hostname = other.getHostname();
        if (this$hostname == null ? other$hostname != null : !this$hostname.equals(other$hostname)) return false;
        final Object this$environment = this.getEnvironment();
        final Object other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment))
            return false;
        if (this.getRetryInterval() != other.getRetryInterval()) return false;
        if (this.getQueueSize() != other.getQueueSize()) return false;
        if (this.getBatchesPerSecond() != other.getBatchesPerSecond()) return false;
        if (this.getBatchSize() != other.getBatchSize()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $sensuHost = this.getSensuHost();
        result = result * PRIME + ($sensuHost == null ? 43 : $sensuHost.hashCode());
        result = result * PRIME + this.getSensuPort();
        final Object $application = this.getApplication();
        result = result * PRIME + ($application == null ? 43 : $application.hashCode());
        final Object $hostname = this.getHostname();
        result = result * PRIME + ($hostname == null ? 43 : $hostname.hashCode());
        final Object $environment = this.getEnvironment();
        result = result * PRIME + ($environment == null ? 43 : $environment.hashCode());
        result = result * PRIME + this.getRetryInterval();
        result = result * PRIME + this.getQueueSize();
        result = result * PRIME + this.getBatchesPerSecond();
        result = result * PRIME + this.getBatchSize();
        return result;
    }

    public String toString() {
        return "MetricsConfig(sensuHost=" + this.getSensuHost() + ", sensuPort=" + this.getSensuPort() + ", application=" + this.getApplication() + ", hostname=" + this.getHostname() + ", environment=" + this.getEnvironment() + ", retryInterval=" + this.getRetryInterval() + ", queueSize=" + this.getQueueSize() + ", batchesPerSecond=" + this.getBatchesPerSecond() + ", batchSize=" + this.getBatchSize() + ")";
    }

    public MetricsConfig withSensuHost(String sensuHost) {
        return this.sensuHost == sensuHost ? this : new MetricsConfig(sensuHost, this.sensuPort, this.application, this.hostname, this.environment, this.retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withSensuPort(int sensuPort) {
        return this.sensuPort == sensuPort ? this : new MetricsConfig(this.sensuHost, sensuPort, this.application, this.hostname, this.environment, this.retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withApplication(String application) {
        return this.application == application ? this : new MetricsConfig(this.sensuHost, this.sensuPort, application, this.hostname, this.environment, this.retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withHostname(String hostname) {
        return this.hostname == hostname ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, hostname, this.environment, this.retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withEnvironment(String environment) {
        return this.environment == environment ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, this.hostname, environment, this.retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withRetryInterval(int retryInterval) {
        return this.retryInterval == retryInterval ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, this.hostname, this.environment, retryInterval, this.queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withQueueSize(int queueSize) {
        return this.queueSize == queueSize ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, this.hostname, this.environment, this.retryInterval, queueSize, this.batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withBatchesPerSecond(int batchesPerSecond) {
        return this.batchesPerSecond == batchesPerSecond ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, this.hostname, this.environment, this.retryInterval, this.queueSize, batchesPerSecond, this.batchSize);
    }

    public MetricsConfig withBatchSize(int batchSize) {
        return this.batchSize == batchSize ? this : new MetricsConfig(this.sensuHost, this.sensuPort, this.application, this.hostname, this.environment, this.retryInterval, this.queueSize, this.batchesPerSecond, batchSize);
    }

    public static class MetricsConfigBuilder {
        private String sensuHost;
        private int sensuPort;
        private String application;
        private String hostname;
        private String environment;
        private int retryInterval;
        private int queueSize;
        private int batchesPerSecond;
        private int batchSize;

        MetricsConfigBuilder() {
        }

        public MetricsConfig.MetricsConfigBuilder sensuHost(String sensuHost) {
            this.sensuHost = sensuHost;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder sensuPort(int sensuPort) {
            this.sensuPort = sensuPort;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder application(String application) {
            this.application = application;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder retryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder queueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder batchesPerSecond(int batchesPerSecond) {
            this.batchesPerSecond = batchesPerSecond;
            return this;
        }

        public MetricsConfig.MetricsConfigBuilder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public MetricsConfig build() {
            return new MetricsConfig(sensuHost, sensuPort, application, hostname, environment, retryInterval, queueSize, batchesPerSecond, batchSize);
        }

        public String toString() {
            return "MetricsConfig.MetricsConfigBuilder(sensuHost=" + this.sensuHost + ", sensuPort=" + this.sensuPort + ", application=" + this.application + ", hostname=" + this.hostname + ", environment=" + this.environment + ", retryInterval=" + this.retryInterval + ", queueSize=" + this.queueSize + ", batchesPerSecond=" + this.batchesPerSecond + ", batchSize=" + this.batchSize + ")";
        }
    }
}
