package no.nav.common.metrics;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static no.nav.common.log.MDCConstants.*;

public class Event {
    final String name;
    final Map<String, Object> fields = new HashMap<>();
    final Map<String, String> tags = new HashMap<>();

    private static final String[] MDC_VARIABLES = {
            MDC_CALL_ID,
            MDC_USER_ID,
            MDC_REQUEST_ID,
            MDC_CONSUMER_ID
    };

    public Event(String name) {
        this.name = name;
        addBaseFields();
    }

    public Event addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
        return this;
    }

    public Event addTagToReport(String tagName, String value) {
        tags.put(tagName, value);
        return this;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Event setSuccess() {
        return addFieldToReport("success", true);
    }

    public Event setFailed() {
        return addFieldToReport("success", false);
    }

    private void addBaseFields() {
        for (String mdcVariable : MDC_VARIABLES) {
            String mdcValue = MDC.get(mdcVariable);
            if(Objects.nonNull(mdcValue)) {
                addFieldToReport(mdcVariable, mdcValue);
            }
        }

        setSuccess();
        addFieldToReport("value", 1);
    }

}
