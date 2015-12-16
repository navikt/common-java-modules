package no.nav.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class MetricProxyUtils {
    private static final Logger logger = LoggerFactory.getLogger(MetricProxyUtils.class);

    @SuppressWarnings("unchecked")
    public static void includeMethodsToMeasure(Object proxyInstance, List<String> methods) {
        InvocationHandler proxyInvocationHandler = Proxy.getInvocationHandler(proxyInstance);
        Class proxyClass = proxyInvocationHandler.getClass();
        try {
            Method method = proxyClass.getMethod("includeMethods", List.class);
            method.invoke(proxyInvocationHandler, methods);
        } catch (Exception e) {
            logger.error("Failed to invoke method", e);
            throw new RuntimeException("Error in Metrics library. Couldn't invoke method includeMethods on " + proxyClass.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public static void excludeMethodsToMeasure(Object proxyInstance, List<String> methods) {
        InvocationHandler proxyInvocationHandler = Proxy.getInvocationHandler(proxyInstance);
        Class proxyClass = proxyInvocationHandler.getClass();
        try {
            Method method = proxyClass.getMethod("excludeMethods", List.class);
            method.invoke(proxyInvocationHandler, methods);
        } catch (Exception e) {
            logger.error("Failed to invoke method", e);
            throw new RuntimeException("Error in Metrics library. Couldn't invoke method excludeMethods on " + proxyClass.getName());
        }
    }
}
