package no.nav.sbl.dialogarena.test.junit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.Properties;

@Slf4j
public class SystemPropertiesRule implements MethodRule {

    private static final Properties INITIAL_SYSTEM_PROPERTIES = copy(System.getProperties());

    private static Properties copy(Properties properties) {
        return (Properties) properties.clone();
    }

    @Override
    @SneakyThrows
    public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    statement.evaluate();
                } finally {
                    System.getProperties().clear();
                    System.setProperties(copy(INITIAL_SYSTEM_PROPERTIES));
                }
            }
        };
    }

    public SystemPropertiesRule setProperty(String name, String value) {
        log.info("{} = {}", name, value);
        System.setProperty(name, value);
        return this;
    }

}
