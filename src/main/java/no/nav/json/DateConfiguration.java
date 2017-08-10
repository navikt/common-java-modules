package no.nav.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalTime.NOON;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static no.nav.json.StringUtils.of;

public class DateConfiguration {

    private static final Map<Class, BaseProvider<?>> converters = new HashMap<>();
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Paris");

    static {
        add(new LocalDateProvider());
        add(new LocalDateTimeProvider());
        add(new ZonedDateTimeProvider());
        add(new DateProvider());
        add(new TimestampProvider());
        add(new SqlDateProvider());
    }

    private static <T> void add(BaseProvider<T> paramConverter) {
        converters.put(paramConverter.targetClass, paramConverter);
    }

    public static Module dateModule() {
        SimpleModule module = new SimpleModule();
        converters.values().forEach((v) -> {
            module.addSerializer(v.serializer);
            module.addDeserializer(v.targetClass, v.deSerializer);
        });
        return module;
    }

    public static ParamConverterProvider parameterConverterProvider() {
        return new ParamConverterProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
                return (ParamConverter<T>) converters.get(rawType);
            }
        };
    }

    private abstract static class BaseProvider<T> implements ParamConverter<T> {

        private final Class targetClass;
        private final JsonSerializer<T> serializer;
        private final JsonDeserializer<T> deSerializer;

        protected abstract T toValue(ZonedDateTime zonedDateTime);

        protected abstract ZonedDateTime from(T value);

        public BaseProvider(Class<T> targetClass) {
            this.targetClass = targetClass;
            this.serializer = new StdScalarSerializer<T>(targetClass) {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeString(BaseProvider.this.toString(value));
                }
            };
            this.deSerializer = new StdScalarDeserializer<T>(targetClass) {

                @Override
                public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    return of(p.getText())
                            .map(BaseProvider.this::fromString)
                            .orElse(null);
                }
            };
        }

        @Override
        public T fromString(String value) {
            return of(value)
                    .map(ZonedDateTime::parse)
                    .map(this::toValue)
                    .orElse(null);
        }

        @Override
        public String toString(T value) {
            return from(value).format(ISO_OFFSET_DATE_TIME);
        }

    }


    private static class LocalDateProvider extends BaseProvider<LocalDate> {

        private LocalDateProvider() {
            super(LocalDate.class);
        }

        @Override
        protected LocalDate toValue(ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDate();
        }

        @Override
        protected ZonedDateTime from(LocalDate value) {
            return value.atTime(NOON).atZone(DEFAULT_ZONE);
        }

    }

    private static class LocalDateTimeProvider extends BaseProvider<LocalDateTime> {

        private LocalDateTimeProvider() {
            super(LocalDateTime.class);
        }

        @Override
        protected LocalDateTime toValue(ZonedDateTime zonedDateTime) {
            return zonedDateTime.withZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
        }

        @Override
        protected ZonedDateTime from(LocalDateTime value) {
            return value.atZone(DEFAULT_ZONE);
        }

    }

    private static class ZonedDateTimeProvider extends BaseProvider<ZonedDateTime> {

        private ZonedDateTimeProvider() {
            super(ZonedDateTime.class);
        }

        @Override
        protected ZonedDateTime toValue(ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        }

        @Override
        protected ZonedDateTime from(ZonedDateTime value) {
            return value;
        }

    }

    private static class DateProvider extends BaseProvider<Date> {

        private DateProvider() {
            super(Date.class);
        }

        @Override
        protected Date toValue(ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }

        @Override
        protected ZonedDateTime from(Date value) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(value.getTime()), DEFAULT_ZONE);
        }

    }

    private static class SqlDateProvider extends BaseProvider<java.sql.Date> {
        private DateProvider dateProvider = new DateProvider();

        public SqlDateProvider() {
            super(java.sql.Date.class);
        }

        @Override
        protected java.sql.Date toValue(ZonedDateTime zonedDateTime) {
            return java.sql.Date.valueOf(zonedDateTime.toLocalDate());
        }

        @Override
        protected ZonedDateTime from(java.sql.Date value) {
            return dateProvider.from(value);
        }
    }


    private static class TimestampProvider extends BaseProvider<java.sql.Timestamp> {
        private DateProvider dateProvider = new DateProvider();

        public TimestampProvider() {
            super(java.sql.Timestamp.class);
        }

        @Override
        protected java.sql.Timestamp toValue(ZonedDateTime zonedDateTime) {
            return java.sql.Timestamp.from(zonedDateTime.toInstant());
        }

        @Override
        protected ZonedDateTime from(java.sql.Timestamp value) {
            return dateProvider.from(value);
        }
    }

}
