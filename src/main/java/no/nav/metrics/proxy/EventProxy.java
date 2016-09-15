package no.nav.metrics.proxy;

import no.nav.metrics.MetodeEvent;

import java.lang.reflect.Method;

public class EventProxy extends MetricProxy {

    public EventProxy(String name, Object object) {
        super(name, object);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ProxyMetodekall metodekall = new ProxyMetodekall(object, method, args);

        if (!shouldMeasureMethod(method.getName())) {
            return metodekall.kallMetode();
        }

        String eventName = name + "." + method.getName();

        return MetodeEvent.eventForMetode(metodekall, eventName);

    }
}
