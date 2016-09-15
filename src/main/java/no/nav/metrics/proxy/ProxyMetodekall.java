package no.nav.metrics.proxy;

import no.nav.metrics.Metodekall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ProxyMetodekall implements Metodekall {

    private final Object object;
    private final Method method;
    private final Object[] args;

    public ProxyMetodekall(Object object, Method method, Object[] args) {
        this.object = object;
        this.method = method;
        this.args = args;
    }

    @Override
    public Object kallMetode() throws Throwable {
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getCause(); // Kast den originale exceptionen
        }
    }
}
