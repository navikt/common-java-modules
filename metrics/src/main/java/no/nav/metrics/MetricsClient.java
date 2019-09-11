package no.nav.metrics;

import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;
import no.nav.util.sbl.EnvironmentUtils;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MetricsClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MetricsClient.class);

    private static volatile boolean metricsReportEnabled;
    private static volatile SensuHandler sensuHandler;
    private static volatile MetricsConfig metricsConfig;

    static {
        if (EnvironmentUtils.isRunningOnJboss()) {
            try {
                enableMetrics(MetricsConfig.resolveSkyaConfig());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.warn("metrics was not automatically enabled");
        }
    }

    public static void enableMetrics(MetricsConfig metricsConfig) {
        if (!metricsReportEnabled) {
            MetricsClient.metricsConfig = metricsConfig;
            sensuHandler = new SensuHandler(metricsConfig);
            metricsReportEnabled = true;
        }
    }

    public static void resetMetrics(MetricsConfig metricsConfig) {
        if (sensuHandler != null) {
            sensuHandler.shutdown();
        }
        metricsReportEnabled = false;
        enableMetrics(metricsConfig);
    }

    void report(String metricName, Map<String, Object> fields, Map<String, String> tagsFromMetric, long timestampInMilliseconds) {
        if (metricsReportEnabled) {
            tagsFromMetric.putIfAbsent("application", metricsConfig.getApplication());
            tagsFromMetric.putIfAbsent("hostname", metricsConfig.getHostname());
            tagsFromMetric.putIfAbsent("environment", metricsConfig.getEnvironment());

            long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
            String output = InfluxHandler.createLineProtocolPayload(metricName, tagsFromMetric, fields, timestamp);
            sensuHandler.report(output);
        }
    }
}
