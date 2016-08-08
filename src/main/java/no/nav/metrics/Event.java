package no.nav.metrics;

import no.nav.metrics.aspects.Count;
import no.nav.metrics.aspects.Field;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
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

    public Event withFields(ProceedingJoinPoint joinPoint, Count count) {
        List<Object> args = asList(joinPoint.getArgs());
        asList(count.fields()).stream()
                .forEach(field -> {
                    String value = args.get(parseInt(field.argumentNumber()) - 1).toString();
                    this.fields.put(field.key(), value);
                });
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