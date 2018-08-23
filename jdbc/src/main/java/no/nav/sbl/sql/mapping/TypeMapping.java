package no.nav.sbl.sql.mapping;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.function.Function;

class TypeMapping {
    public interface Deserializer<FROM, TO> extends Function<FROM, TO> {
    }

    static Map<Class<?>, Map<Class<?>, Deserializer<?, ?>>> typemappers = HashMap.empty();

    static {
        registerDefaults();
    }

    static <FROM, TO> void register(Class<FROM> fromCls, Class<TO> toCls, Deserializer<FROM, TO> deserializer) {
        Map<Class<?>, Deserializer<?, ?>> toMap = TypeMapping.typemappers.get(fromCls).getOrElse(HashMap.empty());
        toMap = toMap.put(toCls, deserializer);
        TypeMapping.typemappers = TypeMapping.typemappers.put(fromCls, toMap);
    }

    @SuppressWarnings("unchecked")
    static  <TO, FROM> TO convert(FROM value, QueryMapping.InternalColumn<FROM, TO> column) {
        if (column.from == column.to) {
            return (TO) value;
        }

        Deserializer<FROM, TO> deserializer = (Deserializer<FROM, TO>) typemappers
                .get(column.from)
                .flatMap((toMap) -> toMap.get(column.to))
                .getOrElseThrow(() -> new IllegalStateException("Could not find serializer for " + column));

        try {
            return deserializer.apply(value);
        } catch (NullPointerException e) {
            if (value == null) {
                return null;
            }
            throw e;
        }
    }

    static void registerDefaults() {
        register(Date.class, LocalDate.class, Date::toLocalDate);
        register(Time.class, LocalTime.class, Time::toLocalTime);
        register(Timestamp.class, LocalDateTime.class, Timestamp::toLocalDateTime);
        register(Timestamp.class, ZonedDateTime.class, (time) -> time.toLocalDateTime().atZone(ZoneId.systemDefault()));

        register(Integer.class, Boolean.class, (value) -> value == 1);
        register(String.class, Boolean.class, (str) -> "J".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str));
    }
}
