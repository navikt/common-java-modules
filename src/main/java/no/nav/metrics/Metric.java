package no.nav.metrics;

import java.util.HashMap;
import java.util.Map;

abstract class Metric<T extends Metric> {
    protected final MetricsClient metricsClient;
    protected final String name;
    protected Map<String, Object> fields = new HashMap<>();
    protected Map<String, String> tags = new HashMap<>();

    Metric(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name;
        setSuccess();
    }

    public T addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
        return self();
    }

    public T addTagToReport(String tagName, String value) {
        tags.put(tagName, value);
        return self();
    }

    public T setSuccess() {
        addFieldToReport("success", true);
        return self();
    }

    public T setFailed() {
        addFieldToReport("success", false);
        return self();
    }

    public abstract T report();

    protected abstract T self();

}
