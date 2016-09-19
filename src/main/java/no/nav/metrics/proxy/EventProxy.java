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

        String metodeNavn = method.getName();
        if (!shouldMeasureMethod(metodeNavn)) {
            return metodekall.kallMetode();
        }

        String eventName = this.name + "." + metodeNavn;

        return MetodeEvent.eventForMetode(metodekall, eventName);

    }
}
