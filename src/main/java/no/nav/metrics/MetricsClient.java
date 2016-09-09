package no.nav.metrics;

import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static no.nav.metrics.MetricsFactory.DISABLE_METRICS_REPORT_KEY;


public class MetricsClient {
    public static final Boolean DISABLE_METRICS_REPORT = parseBoolean(getProperty(DISABLE_METRICS_REPORT_KEY, "false"));
    private final Map<String, String> tags = new HashMap<>();
    private final SensuHandler sensuHandler = new SensuHandler();

    public MetricsClient() {
        addSystemPropertiesToTags();
    }

    private void addSystemPropertiesToTags() {
        tags.put("application", System.getProperty("applicationName"));
        tags.put("hostname", System.getProperty("node.hostname"));
        tags.put("environment", System.getProperty("environment.name"));
    }

    void report(String metricName, Map<String, Object> fields, long timestampInMilliseconds) {
        if (!DISABLE_METRICS_REPORT) {
            long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
            String output = InfluxHandler.createLineProtocolPayload(metricName, tags, fields, timestamp);
            sensuHandler.report(tags.get("application"), output);
        }
    }
}