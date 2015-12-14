package no.nav.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TimerProxy implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetricsClient.class);
    private final Object object;
    private final Map<Method, Timer> methodTimers = new HashMap<>();

    TimerProxy(String name, Object object, Class type) {
        this.object = object;

        Method[] methods = type.getMethods();

        for (Method method : methods) {
            String metricName = name + "." + method.getName();
            Timer timer = MetricsFactory.createTimer(metricName);

            methodTimers.put(method, timer);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        Timer timer = methodTimers.get(method);

        try {
            timer.start();
            Object returnObject = method.invoke(object, args);
            timer.addFieldToReport("success", true);
            return returnObject;
        } catch (InvocationTargetException e) {
            logger.error("Error during invocation of method", e);
            timer.addFieldToReport("success", false);
            throw e;
        } catch (Exception e) {
            logger.error("Exception during invocation of method", e);
            timer.addFieldToReport("success", false);
            throw e;
        } finally {
            timer.stop();
            timer.report();
        }
    }
}
