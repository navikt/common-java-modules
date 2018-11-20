package no.nav.apiapp.rest;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.metrics.MetodeTimer;
import no.nav.metrics.MetricsFactory;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class NavMetricsProvider implements ResourceMethodInvocationHandlerProvider {

    private final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

    @Override
    public InvocationHandler create(Invocable invocable) {
        return this::invokeWithMetrics;
    }

    private Object invokeWithMetrics(Object proxy, Method method, Object[] args) {
        return timer(method).record(() -> MetodeTimer.timeMetode(() -> method.invoke(proxy, args), timerNavn(method)));
    }

    private Timer timer(Method method) {
        return meterRegistry.timer(
                "rest_server",
                "class",
                method.getDeclaringClass().getSimpleName(),
                "method", method.getName()
        );
    }

    private String timerNavn(Method method) {
        return "rest.server." + method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

}
