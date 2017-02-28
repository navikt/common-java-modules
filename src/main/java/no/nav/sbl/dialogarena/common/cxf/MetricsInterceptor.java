package no.nav.sbl.dialogarena.common.cxf;

import no.nav.metrics.MetodeTimer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MetricsInterceptor implements InvocationHandler {

    private final Object target;

    MetricsInterceptor(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return MetodeTimer.timeMetode(() -> method.invoke(target, args), timerNavn(method));
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getTargetException();
        }
    }

    private String timerNavn(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + ".soap";
    }

}
