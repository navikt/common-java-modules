package no.nav.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class MetricProxy implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProxy.class);
    private static final List<String> DO_NOT_MEASURE_METHOD_NAMES = new ArrayList<>(Arrays.asList("hashCode", "equals", "toString"));
    private boolean includedMethodsAreDefined = false;
    private boolean excludedMethodsAreDefined = false;
    private List<String> includedMethodNames;
    private List<String> excludedMethodNames;
    final Object object;

    abstract void initiateMeasurement(String methodName);
    abstract void methodFailedMeasurement(String methodName);
    abstract void endMeasurement(String methodName);

    MetricProxy(Object object) {
        this.object = object;
    }

    void includeMethods(List<String> methodNames) {
        if (excludedMethodsAreDefined) {
            throw new IllegalStateException("Include and exclude are mutual exclusive methods");
        }
        includedMethodNames = new ArrayList<>(methodNames);
        includedMethodsAreDefined = true;
    }

    void excludeMethods(List<String> methodNames) {
        if (includedMethodsAreDefined) {
            throw new IllegalStateException("Include and exclude are mutual exclusive methods");
        }
        excludedMethodNames = new ArrayList<>(methodNames);
        excludedMethodsAreDefined = true;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        String methodName = method.getName();

        if (!shouldMeasureMethod(methodName)) {
            return invokeMethod(method, args);
        }

        try {
            initiateMeasurement(methodName);

            return invokeMethod(method, args);
        } catch (Exception e) {
            methodFailedMeasurement(methodName);
            throw e;
        } finally {
            endMeasurement(methodName);
        }
    }

    protected boolean shouldMeasureMethod(String methodName) {
        if (DO_NOT_MEASURE_METHOD_NAMES.contains(methodName)) {
            return false;
        } else if (includedMethodsAreDefined && !includedMethodNames.contains(methodName)) {
            return false;
        } else if (excludedMethodsAreDefined && excludedMethodNames.contains(methodName)) {
            return false;
        }

        return true;
    }

    protected Object invokeMethod(Method method, Object[] args) throws Exception {
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            LOGGER.info("Method threw exception " + method.toString(), e);
            throw (Exception) e.getCause();
        } catch (Exception e) {
            LOGGER.error("Exception from invoking method " + method.toString(), e);
            throw e;
        }
    }
}
