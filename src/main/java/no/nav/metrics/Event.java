package no.nav.metrics;

import no.nav.metrics.aspects.Count;
import no.nav.metrics.aspects.Field;
import org.aspectj.lang.JoinPoint;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
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

    public Event withFields(JoinPoint joinPoint, Count count) {
        Object[] args = joinPoint.getArgs();

        for (Field field : count.fields()) {
            String value = args[parseInt(field.argumentNumber()) - 1].toString();
            fields.put(field.key(), value);
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