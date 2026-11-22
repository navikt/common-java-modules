package no.nav.common.test.junit;

import java.util.Properties;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class SystemPropertiesRule implements MethodRule {

    private final Properties configuredProperties = new Properties();
    private boolean evaluating;

    private static Properties copy(Properties properties) {
        return (Properties) properties.clone();
    }

    @Override
    public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Properties initialSystemProperties = copy(System.getProperties());
                try {
                    evaluating = true;
                    System.getProperties().putAll(configuredProperties);
                    statement.evaluate();
                } finally {
                    evaluating = false;
                    System.setProperties(initialSystemProperties);
                }
            }
        };
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
