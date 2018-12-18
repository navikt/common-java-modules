package no.nav.apiapp.rest;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.metrics.MetodeTimer;
import no.nav.metrics.MetricsFactory;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class NavMetricsProvider implements ResourceMethodInvocationHandlerProvider, ContainerResponseFilter {

    private static final String METRICS_DATA_PROPERTY = "metrics_data";

    private final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

    @Inject
    private Provider<ContainerRequestContext> containerRequestContextProvider;

    @Override
    public InvocationHandler create(Invocable invocable) {
        return this::invokeWithMetrics;
    }

    private Object invokeWithMetrics(Object proxy, Method method, Object[] args) throws Throwable {
        containerRequestContextProvider.get().setProperty(METRICS_DATA_PROPERTY, new Data(method));
        return MetodeTimer.timeMetode(() -> method.invoke(proxy, args), timerNavn(method));
    }

    private String timerNavn(Method method) {
        return "rest.server." + method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Data data = (Data) requestContext.getProperty(METRICS_DATA_PROPERTY);
        if (data != null) {
            Method method = data.method;
            meterRegistry.timer(
                    "rest_server",
                    "class",
                    method.getDeclaringClass().getSimpleName(),
                    "method",
                    method.getName(),
                    "status",
                    Integer.toString(responseContext.getStatus())
            ).record(System.currentTimeMillis() - data.invocationTimestamp, TimeUnit.MILLISECONDS);
        }
    }

    private static class Data {
        private final long invocationTimestamp = System.currentTimeMillis();
        private final Method method;

        private Data(Method method) {
            this.method = method;
        }
    }
}
