package no.nav.sbl.dialogarena.test.coverage;

import java.lang.reflect.Constructor;
import java.util.function.Function;


public class DeclaredConstructorMadeAccessible implements Function<Class<?>, Constructor<?>> {

    private final Class<?>[] paramTypes;

    public DeclaredConstructorMadeAccessible(Class<?> ... paramTypes) {
        this.paramTypes = paramTypes;
    }

    @Override
    public Constructor<?> apply(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
