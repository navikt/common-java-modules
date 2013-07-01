package no.nav.sbl.dialogarena.test.coverage;

import org.apache.commons.collections15.Transformer;

import java.lang.reflect.Constructor;


public class DeclaredConstructorMadeAccessible implements Transformer<Class<?>, Constructor<?>> {

    private final Class<?>[] paramTypes;

    public DeclaredConstructorMadeAccessible(Class<?> ... paramTypes) {
        this.paramTypes = paramTypes;
    }

    @Override
    public Constructor<?> transform(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
