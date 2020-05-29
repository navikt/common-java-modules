package no.nav.metrics;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import no.nav.sbl.util.EnvironmentUtils;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireNamespace;

@Value
@Wither
@Builder
public class MetricsConfig {

    public static final String SENSU_CLIENT_HOST = "sensu_client_host";
    public static final String SENSU_CLIENT_PORT = "sensu_client_port";

    public static final String SENSU_RETRY_INTERVAL_PROPERTY_NAME = "metrics.sensu.report.retryInterval";
    public static final String SENSU_QUEUE_SIZE_PROPERTY_NAME = "metrics.sensu.report.queueSize";
    public static final String SENSU_BATCHES_PER_SECOND_PROPERTY_NAME = "metrics.sensu.report.batchesPerSecond";
    public static final String SENSU_BATCH_SIZE_PROPERTY_NAME = "metrics.sensu.report.batchSize";
    public static final String SENSU_CONNECT_TIMEOUT_PROPERTY_NAME = "metrics.sensu.report.connectTimeout";

    private String sensuHost;
    private int sensuPort;

    private String application;
    private String hostname;
    private String environment;

    private int retryInterval;
    private int queueSize;
    private int batchesPerSecond;
    private int batchSize;
    private int connectTimeout;

    public static MetricsConfig resolveNaisConfig() {
        return defaultConfig("sensu.nais", 3030);
    }

    private static MetricsConfig defaultConfig(String host, int port) {
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
                .withBatchesPerSecond(defaultIntSystemProperty(SENSU_BATCHES_PER_SECOND_PROPERTY_NAME, 20))
                .withBatchSize(defaultIntSystemProperty(SENSU_BATCH_SIZE_PROPERTY_NAME, 100))
                .withConnectTimeout(defaultIntSystemProperty(SENSU_CONNECT_TIMEOUT_PROPERTY_NAME, 1000));
    }

    private static int defaultIntSystemProperty(String propertyName, int defaultValue) {
        return Integer.parseInt(System.getProperty(propertyName, Integer.toString(defaultValue)));
    }

}