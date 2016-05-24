package no.nav.sbl.dialogarena.test.coverage;

import java.lang.reflect.Constructor;
import java.util.function.Predicate;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;

public final class StaticFinalUtilClass implements Predicate<Class<?>> {

    @Override
    public boolean test(Class<?> type) {
        if (isFinal(type.getModifiers()) && type.getSuperclass() == Object.class) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            if (constructors.length == 1 && constructors[0].getParameterTypes().length == 0) {
                return isPrivate(constructors[0].getModifiers());
            }
        }
        return false;
    }
}