package no.nav.common.test.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;


public class NewInstance implements Function<Constructor<?>, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(NewInstance.class);

    private final Class<?>[] paramTypes;

    public NewInstance(Class<?> ... paramTypes) {
        this.paramTypes = paramTypes;
    }

    @Override
    public Object apply(Constructor<?> constructor) {
        try {
            LOG.info("Instantiating " + constructor.getName());
            return constructor.newInstance(paramTypes);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
