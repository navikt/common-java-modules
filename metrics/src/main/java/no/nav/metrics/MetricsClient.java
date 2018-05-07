package no.nav.metrics;

import lombok.extern.slf4j.Slf4j;
import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;
import no.nav.sbl.util.EnvironmentUtils;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class MetricsClient {

    private static final Map<String, String> SYSTEM_TAGS = new HashMap<>();
    private static volatile boolean metricsReportEnabled;
    private static volatile SensuHandler sensuHandler;

    static {
        if (EnvironmentUtils.isRunningOnJboss()) {
            enableMetrics(MetricsConfig.resoleSkyaConfig());
        } else {
            log.warn("metrics was not automatically enabled");
        }
    }

    public static void enableMetrics(MetricsConfig metricsConfig) {
        if (!metricsReportEnabled) {
            log.info("Enabling metrics with config: {}", metricsConfig);
            SYSTEM_TAGS.put("application", EnvironmentUtils.requireApplicationName());
            SYSTEM_TAGS.put("environment", EnvironmentUtils.requireEnvironmentName());
            SYSTEM_TAGS.put("hostname", EnvironmentUtils.resolveHostName());
            sensuHandler = new SensuHandler(EnvironmentUtils.requireApplicationName(), metricsConfig);
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
            tagsFromMetric.putAll(SYSTEM_TAGS);
            long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
            String output = InfluxHandler.createLineProtocolPayload(metricName, tagsFromMetric, fields, timestamp);
            sensuHandler.report(output);
        }
    }
}