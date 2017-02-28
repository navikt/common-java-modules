package no.nav.sbl.dialogarena.common.cxf;

import org.junit.Test;

import java.lang.reflect.Proxy;

public class MetricsInterceptorTest {

    @Test(expected = IllegalArgumentException.class)
    public void noExceptionWrapping() {
        Intercepted intercepted = interceptorFor(() -> {
            throw new IllegalArgumentException();
        });
        intercepted.call();
    }

    private Intercepted interceptorFor(Intercepted target) {
        return (Intercepted) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{Intercepted.class}, new MetricsInterceptor(target));
    }

    @FunctionalInterface
    private interface Intercepted {
        Object call();
    }

}