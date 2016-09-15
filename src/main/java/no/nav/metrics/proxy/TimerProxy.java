package no.nav.metrics.proxy;

import no.nav.metrics.MetodeTimer;

import java.lang.reflect.Method;

public class TimerProxy extends MetricProxy {
    private String name;

    public TimerProxy(String name, Object object) {
        super(object);
        this.name = name;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        ProxyMetodekall metodekall = new ProxyMetodekall(object, method, args);

        if (!shouldMeasureMethod(method.getName())) {
            return metodekall.kallMetode();
        }

        String timerName = name + "." + method.getName();

        return MetodeTimer.timeMetode(metodekall, timerName);
    }

}
