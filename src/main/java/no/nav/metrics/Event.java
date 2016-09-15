package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private final MetricsClient metricsClient;
    private final String name;
    private Map<String, Object> fields = new HashMap<>();

    Event(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name + ".event";
        addFieldToReport("value", 1);
    }

    public void addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public void report() {
        metricsClient.report(name, fields, System.currentTimeMillis());
    }
}