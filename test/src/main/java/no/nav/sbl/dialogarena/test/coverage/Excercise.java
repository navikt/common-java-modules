package no.nav.sbl.dialogarena.test.coverage;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static no.nav.modig.lang.collections.IterUtils.on;


/**
 * Utilities for å kjøre kode som er utilgjengelig for testing, og/eller
 * fåfengt å teste fordi de er generert, men likevel går utover
 * testdekning.
 */
public final class Excercise {

    private static final Logger LOG = LoggerFactory.getLogger(Excercise.class);

    private final Reflections reflections;

    public Excercise(String ... packageNames) {
        reflections = new Reflections(packageNames, new SubTypesScanner(false));
    }


    public void generatedStaticValueOfMethodInEnums() {
        try {
            for (@SuppressWarnings("rawtypes") Class<? extends Enum> enumClass : reflections.getSubTypesOf(Enum.class)) {
                Method valueOfMethod = enumClass.getMethod("valueOf", String.class);
                valueOfMethod.setAccessible(true);
                for (@SuppressWarnings("rawtypes") Enum constant : enumClass.getEnumConstants()) {
                    LOG.info("Excercising " + enumClass.getName() + ".valueOf(\"" + constant.name() + "\")");
                    Object returnedFromValueOf = valueOfMethod.invoke(enumClass, constant.name());
                    if (returnedFromValueOf != constant) {
                        throw new RuntimeException(
                                "Should have got " + constant + " from invoking valueOf(" + constant.name() +
                                "), but got " + returnedFromValueOf);
                    }
                }
            }
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException  e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void privateDefaultConstructorsInStaticUtilClasses() {
        on(reflections.getSubTypesOf(Object.class))
            .filter(new StaticFinalUtilClass())
            .map(new DeclaredConstructorMadeAccessible())
            .map(new NewInstance())
            .collect();
    }


}
