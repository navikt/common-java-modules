package no.nav.metrics;

import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;

import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static no.nav.metrics.MetricsFactory.DISABLE_METRICS_REPORT_KEY;


public class MetricsClient {
    public static final Boolean DISABLE_METRICS_REPORT = parseBoolean(getProperty(DISABLE_METRICS_REPORT_KEY, "false"));
    private final SensuHandler sensuHandler;
    private final String application;
    private final String hostname;
    private final String environment;

    public MetricsClient() {
        application = System.getProperty("applicationName");
        hostname = System.getProperty("node.hostname");
        environment = System.getProperty("environment.name");

        sensuHandler = new SensuHandler(application);
    }

    void report(String metricName, Map<String, Object> fields, Map<String, String> tagsFromMetric, long timestampInMilliseconds) {
        if (!DISABLE_METRICS_REPORT) {
            tagsFromMetric.putIfAbsent("application", application);
            tagsFromMetric.putIfAbsent("hostname", hostname);
            tagsFromMetric.putIfAbsent("environment", environment);

            long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
            String output = InfluxHandler.createLineProtocolPayload(metricName, tagsFromMetric, fields, timestamp);
            sensuHandler.report(output);
        }
    }
}
