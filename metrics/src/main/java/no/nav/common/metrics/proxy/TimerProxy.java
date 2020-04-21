package no.nav.common.metrics.proxy;

import no.nav.common.metrics.MetodeTimer;

import java.lang.reflect.Method;

public class TimerProxy extends MetricProxy {

    public TimerProxy(String name, Object object) {
        super(name, object);
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
