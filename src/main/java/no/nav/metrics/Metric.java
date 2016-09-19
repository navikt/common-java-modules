package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

abstract class Metric {
    protected final MetricsClient metricsClient;
    protected final String name;
    protected Map<String, Object> fields = new HashMap<>();

    Metric(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name;
    }

    public void addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public void setSuccess() {
        addFieldToReport("success", true);
    }

    public void setFailed() {
        addFieldToReport("success", false);
    }

    public abstract void report();

}
