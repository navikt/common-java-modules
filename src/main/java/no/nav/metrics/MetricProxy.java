package no.nav.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

abstract class MetricProxy implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetricProxy.class);
    private boolean includedMethodsAreDefined = false;
    private boolean excludedMethodsAreDefined = false;
    private List<String> includedMethodNames;
    private List<String> excludedMethodNames;
    final Object object;

    abstract void initiateMeasurement(String s);
    abstract void methodFailedMeasurement(String s);
    abstract void endMeasurement(String s);

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
            return invokeMethod(proxy, method, args);
        }

        try {
            initiateMeasurement(methodName);

            return invokeMethod(proxy, method, args);
        } catch (Exception e) {
            methodFailedMeasurement(methodName);
            throw e;
        } finally {
            endMeasurement(methodName);
        }
    }

    private boolean shouldMeasureMethod(String methodName) {
        if (includedMethodsAreDefined && !includedMethodNames.contains(methodName)) {
            return false;
        } else if (excludedMethodsAreDefined && excludedMethodNames.contains(methodName)) {
            return false;
        }

        return true;
    }

    private Object invokeMethod(Object proxy, Method method, Object[] args) throws Exception {
        try {
            Object returnObject = method.invoke(object, args);
            return returnObject;
        } catch (InvocationTargetException e) {
            logger.error("Error during invocation of method" + method.toString(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Exception during invocation of method" + method.toString(), e);
            throw e;
        }
    }
}
