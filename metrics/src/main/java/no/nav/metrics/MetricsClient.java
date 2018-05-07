package no.nav.metrics;

import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;
import no.nav.sbl.util.EnvironmentUtils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static no.nav.metrics.MetricsFactory.DISABLE_METRICS_REPORT_KEY;


public class MetricsClient {
    public static final Boolean DISABLE_METRICS_REPORT = parseBoolean(getProperty(DISABLE_METRICS_REPORT_KEY, "false"));
    private final Map<String, String> systemTags = new HashMap<>();
    private final SensuHandler sensuHandler;

    public MetricsClient() {
        addSystemPropertiesToTags();
        sensuHandler = new SensuHandler(systemTags.get("application"));
    }

    private void addSystemPropertiesToTags() {
        systemTags.put("application", EnvironmentUtils.requireApplicationName());
        systemTags.put("environment", EnvironmentUtils.requireEnvironmentName());
        systemTags.put("hostname", EnvironmentUtils.resolveHostName());
    }

    void report(String metricName, Map<String, Object> fields, Map<String, String> tagsFromMetric, long timestampInMilliseconds) {
        tagsFromMetric.putAll(systemTags);
        if (!DISABLE_METRICS_REPORT) {
            long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
            String output = InfluxHandler.createLineProtocolPayload(metricName, tagsFromMetric, fields, timestamp);
            sensuHandler.report(output);
        }
    }
}