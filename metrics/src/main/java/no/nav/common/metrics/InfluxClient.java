package no.nav.common.metrics;

import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class InfluxClient implements MetricsClient {

    private final SensuHandler sensuHandler;

    private final SensuConfig sensuConfig;

    public InfluxClient() {
        this(SensuConfig.defaultConfig());
    }

    public InfluxClient(SensuConfig sensuConfig) {
        this.sensuConfig = sensuConfig;
        this.sensuHandler = new SensuHandler(sensuConfig);
    }

    public InfluxClient(SensuConfig sensuConfig, SensuHandler sensuHandler) {
        this.sensuConfig = sensuConfig;
        this.sensuHandler = sensuHandler;
    }

    public void shutdown() {
        sensuHandler.shutdown();
    }

    @Override
    public void report(Event event) {
        report(event.name, event.fields, event.tags, System.currentTimeMillis());
    }

    @Override
    public void report(String eventName, Map<String, Object> fields, Map<String, String> tags, long timestampInMilliseconds) {
        tags.putIfAbsent("application", sensuConfig.getApplication());
        tags.putIfAbsent("hostname", sensuConfig.getHostname());
        tags.putIfAbsent("cluster", sensuConfig.getCluster());
        tags.putIfAbsent("namespace", sensuConfig.getNamespace());

        long timestamp = MILLISECONDS.toNanos(timestampInMilliseconds);
        String output = InfluxUtils.createInfluxLineProtocolPayload(eventName, tags, fields, timestamp);
        sensuHandler.report(output);
    }

}
