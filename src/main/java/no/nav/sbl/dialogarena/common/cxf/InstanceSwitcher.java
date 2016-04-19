package no.nav.sbl.dialogarena.common.cxf;

import no.nav.modig.core.exception.ApplicationException;
import org.slf4j.Logger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.lang.reflect.Proxy.newProxyInstance;
import static no.nav.metrics.MetricsFactory.createTimerProxyForWebService;
import static org.slf4j.LoggerFactory.*;

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

    public static <T> T createMetricsProxyWithInstanceSwitcher(String name, T prod, T mock, String key, Class<T> type) {
        T switcher = createSwitcher(prod, mock, key, type);
        return createTimerProxyForWebService(name, switcher, type);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        method.setAccessible(true);
        try {
            if (getProperty(key, "false").equalsIgnoreCase("true")) {
                if (getProperty(key + ".simulate.error", "false").equalsIgnoreCase("true")) {
                    throw new ApplicationException("Simulerer exception ved kall til tjenesten.");
                }
                return method.invoke(alternative, args);
            }
            return method.invoke(defaultInstance, args);
        } catch(InvocationTargetException exception){
            LOG.info("invokasjon feiler, kaster reell exception", exception);
            throw exception.getCause();
        }catch (IllegalAccessException exception) {
            throw new ApplicationException("Problemer med invokering av metode", exception);
        }
    }
    public String getTargetClassName() {
        return alternative.getClass().getName().split("\\$")[0];
    }

    private static boolean mockSetupErTillatt() {
        return valueOf(getProperty(TILLATMOCK_PROPERTY, DEFAULT_MOCK_TILATT));
    }
}
