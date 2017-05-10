package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

abstract class Metric {
    protected final MetricsClient metricsClient;
    protected final String name;
    protected Map<String, Object> fields = new HashMap<>();
    protected Map<String, String> tags = new HashMap<>();

    Metric(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name;
        setSuccess();
    }

    public void addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    public void addTagToReport(String tagName, String value) {
        tags.put(tagName, value);
    }

    public void setSuccess() {
        addFieldToReport("success", true);
    }

    public void setFailed() {
        addFieldToReport("success", false);
    }

    public abstract void report();

}
