package no.nav.metrics;

import no.nav.metrics.proxy.EventProxy;
import no.nav.metrics.proxy.TimerProxy;

import static java.lang.reflect.Proxy.newProxyInstance;

public class MetricsFactory {
    public static final String DISABLE_METRICS_REPORT_KEY = "disable.metrics.report";
    public static final String DISABLE_SYSTEM_TAGS_KEY = "disable.metrics.systemtags";
    private static final MetricsClient metricsClient = new MetricsClient();

    public static Timer createTimer(String name) {
        return new Timer(metricsClient, name);
    }

    public static Event createEvent(String name) {
        return new Event(metricsClient, name);
    }

    public static <T> T createTimerProxy(String name, T object, Class<T> type) {
        return createTimerProxyInstance(name, object, type);
    }

    public static <T> T createTimerProxyForWebService(String name, T object, Class<T> type) {
        return createTimerProxyInstance("ws." + name, object, type);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createTimerProxyInstance(String name, T object, Class<T> type) {
        ClassLoader classLoader = TimerProxy.class.getClassLoader();
        Class[] classes = {type};
        TimerProxy timerProxy = new TimerProxy(name, object);

        return (T) newProxyInstance(classLoader, classes, timerProxy);
    }

    @SuppressWarnings("unchecked")
    public static <T> T createEventProxy(String name, T object, Class<T> type) {
        ClassLoader classLoader = EventProxy.class.getClassLoader();
        Class[] classes = {type};
        EventProxy eventProxy = new EventProxy(name, object);

        return (T) newProxyInstance(classLoader, classes, eventProxy);
    }
}
