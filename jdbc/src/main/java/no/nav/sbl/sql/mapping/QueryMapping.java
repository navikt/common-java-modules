package no.nav.sbl.sql.mapping;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Value;
import no.nav.sbl.sql.SelectQuery;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.Arrays;

public class QueryMapping<T extends SqlRecord> {
    private static List<String> ignoreFields = List.of("$jacocoData");
    private static Map<Class<? extends SqlRecord>, QueryMapping<? extends SqlRecord>> mappers = HashMap.empty();
    private Class<T> targetClass;
    private List<InternalColumn> columns;
    private Constructor<T> constructor;

    private QueryMapping(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.columns = getColumns();
        this.constructor = getConstructor();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SqlRecord> QueryMapping<T> of(Class<T> targetClass) {
        if (!mappers.containsKey(targetClass)) {
            mappers = mappers.put(targetClass, new QueryMapping<>(targetClass));
        }

        return (QueryMapping<T>) mappers.get(targetClass).getOrNull();
    }

    public SelectQuery<T> applyColumn(SelectQuery<T> query) {
        columns.forEach((column) -> query.column(column.name));
        return query;
    }

    public T createMapper(ResultSet rs) {
        return Try.of(() -> {
            Object[] parameters = columns.map((column) -> deserialize(column, rs)).toJavaArray();

            return constructor.newInstance(parameters);
        }).getOrElseThrow((err) -> new RuntimeException("Failed to deserialize", err));
    }

    public static <FROM, TO> void register(Class<FROM> fromCls, Class<TO> toCls, TypeMapping.Deserializer<FROM, TO> deserializer) {
        TypeMapping.register(fromCls, toCls, deserializer);
    }

    private <FROM, TO> Column<FROM, TO> deserialize(InternalColumn<FROM, TO> column, ResultSet rs) {
        FROM value = ValueMapping.getValue(column, rs);
        return Column.of(TypeMapping.convert(value, column));
    }

    @SuppressWarnings("unchecked")
    private List<InternalColumn> getColumns() {
        List<Field> fields = List.of(targetClass.getDeclaredFields())
                .filter((field) -> !ignoreFields.contains(field.getName()));

        verifyFields(fields);

        return fields
                .map((field) -> {
                    String name = field.getName();
                    Type[] genericTypes = ((ParameterizedType) field.getAnnotatedType().getType()).getActualTypeArguments();
                    Class from = ((Class) genericTypes[0]);
                    Class to = ((Class) genericTypes[1]);
                    return new InternalColumn(name, from, to);
                });
    }

    @SuppressWarnings("unchecked")
    private Constructor<T> getConstructor() {
        InternalColumn[] parameterTypes = this.columns.toJavaArray(InternalColumn.class);

        Constructor<T> constructor = findConstructorWithParamLength(targetClass, parameterTypes.length);
        verifyConstructorParameters(constructor, parameterTypes);

        return constructor;
    }

    private static void verifyFields(List<Field> fields) {
        Option<Field> brokenField = fields
                .find((field) -> !Column.class.isAssignableFrom(field.getType()));


        if (brokenField.isDefined()) {
            throw new IllegalArgumentException("targetClass contains non-column fields " + brokenField);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findConstructorWithParamLength(Class<T> targetClass, int targetLength) {
        List<Constructor<?>> constructors = List.of(targetClass.getConstructors())
                .filter((constructor) -> constructor.getParameterCount() == targetLength);

        if (constructors.length() != 1) {
            throw new IllegalArgumentException("Found " + constructors.length() + " constructors with " + targetLength + "parameters. Expected just one.");
        }

        return (Constructor<T>) constructors.get(0);
    }

    private static <T> void verifyConstructorParameters(Constructor<T> constructor, InternalColumn[] parameterTypes) {
        List<Type> constructorTypes = List.of(constructor.getGenericParameterTypes());

        verifyParameterBaseType(constructorTypes, parameterTypes);
        verifyParameterGenericType(constructorTypes, parameterTypes);
    }

    private static void verifyParameterBaseType(List<Type> constructorTypes, InternalColumn[] parameterTypes) {
        int nonGenericConstructorTypes = constructorTypes
                .filter((cls) -> !(cls instanceof ParameterizedType))
                .length();

        if (nonGenericConstructorTypes > 0) {
            String msg = "Constructor parameter mismatch, expected " + Arrays.toString(parameterTypes) + " found: " + constructorTypes;
            throw new IllegalArgumentException(msg);
        }
    }

    private static void verifyParameterGenericType(List<Type> constructorTypes, InternalColumn[] parameterTypes) {
        List<Tuple2<Class, Class>> constructorGenericTypes = constructorTypes
                .map((param) -> {
                    Type[] genericTypes = ((ParameterizedType) param).getActualTypeArguments();
                    Class from = ((Class) genericTypes[0]);
                    Class to = ((Class) genericTypes[1]);
                    return Tuple.of(from, to);
                });

        for (int i = 0; i < parameterTypes.length; i++) {
            InternalColumn paramType = parameterTypes[i];
            Tuple2<Class, Class> constructorType = constructorGenericTypes.get(i);

            if (!(paramType.from.equals(constructorType._1) && paramType.to.equals(constructorType._2))) {
                String msg = "Constructor parameter mismatch, expected " + Arrays.toString(parameterTypes) + " found: " + constructorTypes;
                throw new IllegalArgumentException(msg);
            }
        }
    }

    @Value(staticConstructor = "of")
    static class InternalColumn<FROM, TO> {
        public String name;
        public Class<FROM> from;
        public Class<TO> to;
    }

    @Value(staticConstructor = "of")
    public static class Column<FROM, TO> {
        public TO value;
    }
}
