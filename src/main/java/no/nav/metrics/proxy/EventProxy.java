package no.nav.metrics.proxy;

import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EventProxy extends MetricProxy {
    private final Map<String, Event> methodEvents = new HashMap<>();

    public EventProxy(String name, Object object, Class type) {
        super(object);

        Method[] methods = type.getMethods();

        for (Method method : methods) {
            String metricName = name + "." + method.getName();
            Event event = MetricsFactory.createEvent(metricName);

            methodEvents.put(method.getName(), event);
        }

    }
    @Override
    void initiateMeasurement(String methodName) {
        Event event = methodEvents.get(methodName);
        event.addFieldToReport("success", true);
    }

    @Override
    void methodFailedMeasurement(String methodName) {
        Event event = methodEvents.get(methodName);
        event.addFieldToReport("success", false);
    }

    @Override
    void endMeasurement(String methodName) {
        Event event = methodEvents.get(methodName);
        event.report();
    }
}
