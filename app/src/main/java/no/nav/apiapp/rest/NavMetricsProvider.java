package no.nav.apiapp.rest;

import no.nav.metrics.MetodeTimer;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class NavMetricsProvider implements ResourceMethodInvocationHandlerProvider {

    @Override
    public InvocationHandler create(Invocable invocable) {
        return (proxy, method, args) -> MetodeTimer.timeMetode(() -> method.invoke(proxy, args), timerNavn(method));
    }

    private String timerNavn(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + ".rest";
    }

}
