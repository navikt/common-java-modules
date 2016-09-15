package no.nav.metrics.proxy;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;

import java.lang.reflect.Method;

public class TimerProxy extends MetricProxy {
    private String name;

    public TimerProxy(String name, Object object) {
        super(object);
        this.name = name;
    }

    /**
     * Midlertidig implementasjon av TimerProxy for å hindre at det oppstår race conditions dersom
     * det gjøres målinger på samme metodekall samtidig, dvs at måling på kall 2 starter før kall 1 er ferdig og
     * rapportert. Store deler av metrics-biblioteket må sannsynligvis skrives om. I dag er det bygget opp med
     * grunntanken at Metric-implementasjoner kan gjenbrukes, noe som i praksis viste seg å ikke være riktig.
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
        String methodName = method.getName();

        if (!shouldMeasureMethod(methodName)) {
            return invokeMethod(method, args);
        }

        String metricName = name + "." + method.getName();
        Timer timer = MetricsFactory.createTimer(metricName);

        try {
            timer.start();
            timer.addFieldToReport("success", true);
            return invokeMethod(method, args);
        } catch (Exception e) {
            timer.addFieldToReport("success", false);
            throw e;
        } finally {
            timer.stop();
            timer.report();
        }
    }

    @Override
    void initiateMeasurement(String methodName) {
        /* This implementation is intentionally left blank. */
    }

    @Override
    void methodFailedMeasurement(String methodName) {
        /* This implementation is intentionally left blank. */
    }

    @Override
    void endMeasurement(String methodName) {
        /* This implementation is intentionally left blank. */
    }
}
