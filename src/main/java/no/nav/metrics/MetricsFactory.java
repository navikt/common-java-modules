package no.nav.metrics;

import static java.lang.reflect.Proxy.newProxyInstance;

public class MetricsFactory {
    private static final MetricsClient metricsClient = new MetricsClient();

    public static Timer createTimer(String name) {
        return new Timer(metricsClient, name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T createTimerProxy(String name, T object, Class<T> type) {
        ClassLoader classLoader = TimerProxy.class.getClassLoader();
        Class[] classes = {type};
        TimerProxy timerProxy = new TimerProxy(name, object, type);

        return (T) newProxyInstance(classLoader, classes, timerProxy);
    }
}
