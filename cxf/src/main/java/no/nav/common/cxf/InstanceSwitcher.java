package no.nav.common.cxf;

import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.slf4j.LoggerFactory.getLogger;

public final class InstanceSwitcher implements InvocationHandler {

    public static final String TILLATMOCK_PROPERTY = "tillatmock";
    private static final String DEFAULT_MOCK_TILATT = "false";

    private static final Logger LOG = getLogger(InstanceSwitcher.class);
    private final Object defaultInstance;
    private final Object alternative;
    private final String key;

    private <T> InstanceSwitcher(T defaultInstance, T alternative, String key) {
        this.defaultInstance = defaultInstance;
        this.alternative = alternative;
        this.key = key;
    }

    public static <T> T createSwitcher(T defaultInstance, T alternative, String key, Class<T> type) {
        if (!mockSetupErTillatt()) {
            return defaultInstance;
        }

        return (T) newProxyInstance(
                InstanceSwitcher.class.getClassLoader(),
                new Class[]{type},
                new InstanceSwitcher(defaultInstance, alternative, key)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        method.setAccessible(true);
        try {
            if (getProperty(key, "false").equalsIgnoreCase("true")) {
                if (getProperty(key + ".simulate.error", "false").equalsIgnoreCase("true")) {
                    throw new RuntimeException("Simulerer exception ved kall til tjenesten.");
                }
                return method.invoke(alternative, args);
            }
            return method.invoke(defaultInstance, args);
        } catch(InvocationTargetException exception){
            LOG.info("invokasjon feiler, kaster reell exception", exception);
            throw exception.getCause();
        }catch (IllegalAccessException exception) {
            throw new RuntimeException("Problemer med invokering av metode", exception);
        }
    }
    public String getTargetClassName() {
        return alternative.getClass().getName().split("\\$")[0];
    }

    private static boolean mockSetupErTillatt() {
        return valueOf(getProperty(TILLATMOCK_PROPERTY, DEFAULT_MOCK_TILATT));
    }
}
