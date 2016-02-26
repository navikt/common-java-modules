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
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (!shouldMeasureMethod(methodName)) {
            return invokeMethod(method, args);
        }

        try {
            initiateMeasurement(methodName);

            return invokeMethod(method, args);
        } catch (Exception e) {
            methodFailedMeasurement(methodName);
            throw e.getCause();
        } finally {
            endMeasurement(methodName);
        }
    }

    private boolean shouldMeasureMethod(String methodName) {
        if (DO_NOT_MEASURE_METHOD_NAMES.contains(methodName)) {
            return false;
        } else if (includedMethodsAreDefined && !includedMethodNames.contains(methodName)) {
            return false;
        } else if (excludedMethodsAreDefined && excludedMethodNames.contains(methodName)) {
            return false;
        }

        return true;
    }

    private Object invokeMethod(Method method, Object[] args) throws Exception {
        try {
            Object returnObject = method.invoke(object, args);
            return returnObject;
        } catch (InvocationTargetException e) {
            LOGGER.error("Error during invocation of method" + method.toString(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during invocation of method" + method.toString(), e);
            throw e;
        }
    }
}
