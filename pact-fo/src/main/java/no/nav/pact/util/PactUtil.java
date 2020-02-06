package no.nav.pact.util;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class PactUtil {

    private static Map<Class<?>, DSLBuilder> map = new HashMap<>();

    static {
        register(String.class, PactUtil::addString);
        register(boolean.class, PactUtil::addBoolean);
        register(int.class, PactUtil::addInt);
    }

    private static void addBoolean(Field field, boolean aBoolean, PactDslJsonBody lambdaDslJsonBody) {
        lambdaDslJsonBody.booleanType(field.getName(), aBoolean);
    }

    private static void addInt(Field field, int anInt, PactDslJsonBody lambdaDslJsonBody) {
        lambdaDslJsonBody.integerType(field.getName(), anInt);
    }

    private static void addString(Field field, String value, PactDslJsonBody lambdaDslJsonBody) {
        lambdaDslJsonBody.stringType(field.getName(), value);
    }

    private static <T> void register(Class<T> tClass, DSLBuilder<T> dslBuilder) {
        map.put(tClass, dslBuilder);
    }

    public static PactDslJsonBody dtoBody(Object object) {
        Class<?> aClass = object.getClass();
        PactDslJsonBody pactDslJsonBody = new PactDslJsonBody();
        Arrays.stream(aClass.getDeclaredFields()).forEach(field -> requireField(object, pactDslJsonBody, field));
        return pactDslJsonBody;
    }

    @SneakyThrows
    private static void requireField(Object object, PactDslJsonBody pactDslJsonBody, Field field) {
        if (field.isSynthetic()) { // f.eks. $jacocoData
            return;
        } else {
            field.setAccessible(true);
            Object value = field.get(object);
            ofNullable(map.get(field.getType()))
                    .orElseThrow(() -> new IllegalArgumentException(String.format("could not resolve builder for type=%s for field %s", field.getType(), field.getName())))
                    .requireField(field, value, pactDslJsonBody);
        }
    }

    public interface DSLBuilder<T> {
        void requireField(Field field, T value, PactDslJsonBody body);
    }

}
