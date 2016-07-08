package no.nav.metrics;

import no.nav.metrics.aspects.Field;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Event implements Metric {
    private final MetricsClient metricsClient;
    private final String name;
    private Map<String, Object> fields = new HashMap<>();

    Event(MetricsClient metricsClient, String name) {
        this.metricsClient = metricsClient;
        this.name = name + ".event";
        addFieldToReport("value", 1);
    }

    public Event withFields(Field[] fields) {
        for (Field field : fields) {
            this.fields.put(field.key(), field.value());
        }
        return this;
    }

    @Override
    public void addFieldToReport(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    @Override
    public void report() {
        long currentTime = System.currentTimeMillis();
        long measureTimestamp = MILLISECONDS.toSeconds(currentTime);

        metricsClient.report(name, fields, measureTimestamp);
    }
}