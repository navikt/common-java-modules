package no.nav.common.metrics.proxy;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MetricProxy implements InvocationHandler {
    public static final List<String> DO_NOT_MEASURE_METHOD_NAMES = new ArrayList<>(Arrays.asList("hashCode", "equals", "toString"));

    private boolean includedMethodsAreDefined = false;
    private boolean excludedMethodsAreDefined = false;
    private List<String> includedMethodNames;
    private List<String> excludedMethodNames;

    final Object object;
    final String name;

    MetricProxy(String name, Object object) {
        this.name = name;
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
