package no.nav.common.metrics;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import no.nav.common.utils.EnvironmentUtils;

import static no.nav.common.utils.EnvironmentUtils.*;

@Value
@Wither
@Builder
public class SensuConfig {

    public static final String SENSU_CLIENT_HOST = "sensu_client_host";
    public static final String SENSU_CLIENT_PORT = "sensu_client_port";

    public static final String SENSU_RETRY_INTERVAL_PROPERTY_NAME = "metrics.sensu.report.retryInterval";
    public static final String SENSU_QUEUE_SIZE_PROPERTY_NAME = "metrics.sensu.report.queueSize";
    public static final String SENSU_BATCHES_PER_SECOND_PROPERTY_NAME = "metrics.sensu.report.batchesPerSecond";
    public static final String SENSU_BATCH_SIZE_PROPERTY_NAME = "metrics.sensu.report.batchSize";

    private String sensuHost;
    private int sensuPort;

    private String application;
    private String hostname;
    private String cluster;
    private String namespace;

    private int retryInterval;
    private int queueSize;
    private int batchesPerSecond;
    private int batchSize;

    public static SensuConfig resolveNaisConfig() {
        return defaultConfig("sensu.nais", 3030);
    }

    private static SensuConfig defaultConfig(String host, int port) {
        return withSensuDefaults(SensuConfig.builder()
                .sensuHost(getOptionalProperty(SENSU_CLIENT_HOST).orElse(host))
                .sensuPort(getOptionalProperty(SENSU_CLIENT_PORT).map(Integer::parseInt).orElse(port))
                .application(requireApplicationName())
                .cluster(requireClusterName())
                .namespace(requireNamespace())
                .hostname(EnvironmentUtils.resolveHostName())
                .build()
        );
    }

    public static SensuConfig withSensuDefaults(SensuConfig sensuConfig) {
        return sensuConfig
                .withRetryInterval(defaultIntSystemProperty(SENSU_RETRY_INTERVAL_PROPERTY_NAME, 1000))
                .withQueueSize(defaultIntSystemProperty(SENSU_QUEUE_SIZE_PROPERTY_NAME, 20_000))
                .withBatchesPerSecond(defaultIntSystemProperty(SENSU_BATCHES_PER_SECOND_PROPERTY_NAME, 50))
                .withBatchSize(defaultIntSystemProperty(SENSU_BATCH_SIZE_PROPERTY_NAME, 100));
    }

    private static int defaultIntSystemProperty(String propertyName, int defaultValue) {
        return Integer.parseInt(System.getProperty(propertyName, Integer.toString(defaultValue)));
    }

}
