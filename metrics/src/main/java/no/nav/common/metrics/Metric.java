package no.nav.common.metrics;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static no.nav.common.log.MDCConstants.*;


abstract class Metric<T extends Metric> {
    protected final MetricsClient metricsClient;
    protected final String name;
    protected Map<String, Object> fields = new HashMap<>();
    protected Map<String, String> tags = new HashMap<>();

    private static final String[] MDC_VARIABLES = {
            MDC_CALL_ID,
            MDC_USER_ID,
            MDC_REQUEST_ID,
            MDC_CONSUMER_ID
    };

    Metric(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name;
        setSuccess();
        for (String mdcVariable : MDC_VARIABLES) {
            String mdcValue = MDC.get(mdcVariable);
            if(Objects.nonNull(mdcValue)) {
                addFieldToReport(mdcVariable, mdcValue);
            }
        }
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
