package no.nav.metrics;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TimerProxy extends MetricProxy {
    private final Map<String, Timer> methodTimers = new HashMap<>();

    TimerProxy(String name, Object object, Class type) {
        super(object);

        Method[] methods = type.getMethods();

        for (Method method : methods) {
            String metricName = name + "." + method.getName();
            Timer timer = MetricsFactory.createTimer(metricName);

            methodTimers.put(method.getName(), timer);
        }
    }

    @Override
    void initiateMeasurement(String methodName) {
        Timer timer = methodTimers.get(methodName);

        timer.start();
        timer.addFieldToReport("success", true);
    }

    @Override
    void methodFailedMeasurement(String methodName) {
        Timer timer = methodTimers.get(methodName);

        timer.addFieldToReport("success", false);
    }

    @Override
    void endMeasurement(String methodName) {
        Timer timer = methodTimers.get(methodName);

        timer.stop();
        timer.report();
    }
}
