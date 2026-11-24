package no.nav.common.test.junit;

import java.util.Properties;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;

public class SystemPropertiesRule implements BeforeEachCallback, AfterEachCallback {

    private final Properties configuredProperties = new Properties();
    private Properties initialSystemProperties;
    private boolean evaluating;

    private static Properties copy(Properties properties) {
        return (Properties) properties.clone();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        initialSystemProperties = copy(System.getProperties());
        evaluating = true;
        System.getProperties().putAll(configuredProperties);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        evaluating = false;
        System.setProperties(initialSystemProperties);
    }

    public void evaluate(Executable executable) throws Throwable {
        beforeEach(null);
        try {
            executable.execute();
        } finally {
            afterEach(null);
        }
    }

    public SystemPropertiesRule setProperty(String name, String value) {
        if (evaluating) {
            System.setProperty(name, value);
        } else {
            configuredProperties.setProperty(name, value);
        }
        return this;
    }
}
