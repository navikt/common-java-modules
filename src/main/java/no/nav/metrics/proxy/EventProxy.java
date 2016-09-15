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
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;// TODO fix for Event også

    }
}
