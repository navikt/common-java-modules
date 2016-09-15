package no.nav.metrics.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.util.*;

public abstract class MetricProxy implements InvocationHandler {
    public static final List<String> DO_NOT_MEASURE_METHOD_NAMES = new ArrayList<>(Arrays.asList("hashCode", "equals", "toString"));

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProxy.class);

    private boolean includedMethodsAreDefined = false;
    private boolean excludedMethodsAreDefined = false;
    private List<String> includedMethodNames;
    private List<String> excludedMethodNames;
    final Object object;

    MetricProxy(Object object) {
        this.object = object;
    }

    public void includeMethods(List<String> methodNames) {
        if (excludedMethodsAreDefined) {
            throw new IllegalStateException("Include and exclude are mutual exclusive methods");
        }
        includedMethodNames = new ArrayList<>(methodNames);
        includedMethodsAreDefined = true;
    }

    public void excludeMethods(List<String> methodNames) {
        if (includedMethodsAreDefined) {
            throw new IllegalStateException("Include and exclude are mutual exclusive methods");
        }
        excludedMethodNames = new ArrayList<>(methodNames);
        excludedMethodsAreDefined = true;
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

}
