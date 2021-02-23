package no.nav.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Data;
import lombok.experimental.Accessors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.asList;
import static no.nav.common.json.DateModule.DEFAULT_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JsonUtilsTest {
    private static final String TEST_MAP_OF_MAP = "{\"app\": {\"key\": true}, \"app2\": {\"key2\": false}}";
    private static final String TEST_OBJECT_JSON = "{\"aString\":\"test\",\"enEnum\":\"ABC\",\"date\":\"2017-08-09T13:49:13.816+02:00\"}";
    private static final String EMPTY_ENUM_VALUE_JSON = "{\"aString\":\"test\",\"enEnum\":\"\"}";

    @Nested
    class toJson {
        @Test
        public void nullverdi() {
            Assertions.assertThat(JsonUtils.toJson(null)).isNull();
        }

        @Test
        public void empty() {
            Assertions.assertThat(JsonUtils.toJson("")).isEqualTo("\"\"");
        }

        @Test
        public void string() {
            Assertions.assertThat(JsonUtils.toJson("a string")).isEqualTo("\"a string\"");
        }

        @Test
        public void object() {
            Assertions.assertThat(JsonUtils.toJson(new TestObject())).isEqualTo(TEST_OBJECT_JSON);
        }

        @Test
        public void circularObject() {
            assertThatThrownBy(() -> JsonUtils.toJson(new CircularObject())).isInstanceOf(JsonMappingException.class);
        }
    }

    @Nested
    class fromJson {

        @Test
        public void objekt() {
            Assertions.assertThat(JsonUtils.fromJson(TEST_OBJECT_JSON, TestObject.class)).isEqualTo(new TestObject());
        }

        @Test
        public void inputstream() {
            Assertions.assertThat(JsonUtils.fromJson(new ByteArrayInputStream(TEST_OBJECT_JSON.getBytes()), TestObject.class)).isEqualTo(new TestObject());
        }

        @Test
        public void typereference() {
            TypeReference<Map<String, Map<String, Boolean>>> type = new TypeReference<>() {};
            Map<String, Map<String, Boolean>> map = JsonUtils.fromJson(TEST_MAP_OF_MAP, type);

            assertThat(map.get("app").get("key")).isTrue();
            assertThat(map.get("app2").get("key2")).isFalse();
        }


        @Test
        public void emptyEnumString_null() {
            TestObject testObject = JsonUtils.fromJson(EMPTY_ENUM_VALUE_JSON, TestObject.class);
            assertThat(testObject.enEnum).isNull();
        }


        @Test
        public void localDatePaaFormat_yyyy_MM_dd() {
            TestLocalDate testDato = JsonUtils.fromJson("{\"dato\":\"2018-05-09\"}", TestLocalDate.class);
            assertThat(testDato.dato).isEqualTo(LocalDate.of(2018, 5, 9));
        }

        @Test
        public void localDateIsNull() {
            TestLocalDate testDato = JsonUtils.fromJson("{\"dato\":null}", TestLocalDate.class);
            assertThat(testDato.dato).isNull();
        }

        @Test
        public void localDateTime_uten_offset() {
            TestLocalDateTime testDato = JsonUtils.fromJson("{\"dato\":\"2021-01-18T09:48:58.762\"}", TestLocalDateTime.class);
            assertThat(testDato.dato).isEqualTo(LocalDateTime.of(2021, 1, 18, 9, 48, 58).plus(762, MILLIS));
        }

        @Test
        public void localDateTime_eldre_dato() {
            TestLocalDateTime testDato = JsonUtils.fromJson("{\"dato\":\"1957-01-18T09:48:58.762\"}", TestLocalDateTime.class);
            assertThat(testDato.dato).isEqualTo(LocalDateTime.of(1957, 1, 18, 9, 48, 58).plus(762, MILLIS));
        }

        @Test
        public void localDateTime_med_offset() {
            TestLocalDateTime testDato = JsonUtils.fromJson("{\"dato\":\"2021-01-26T09:50:07.838+01:00\"}", TestLocalDateTime.class);
            assertThat(testDato.dato).isEqualTo(LocalDateTime.of(2021, 1, 26, 9, 50, 7).plus(838, MILLIS));
        }

        @Test
        public void zonedDateTime() {
            TestZonedDateTime testDato = JsonUtils.fromJson("{\"dato\":\"2021-01-26T09:50:07.838+01:00\"}", TestZonedDateTime.class);
            assertThat(testDato.dato).isEqualTo(ZonedDateTime.of(LocalDateTime.of(2021, 1, 26, 9, 50, 7).plus(838, MILLIS), DEFAULT_ZONE));
        }

        @Test
        public void zonedDateTime_eldre_dato() {
            TestZonedDateTime testDato = JsonUtils.fromJson("{\"dato\":\"1947-01-26T09:50:07.838+01:00\"}", TestZonedDateTime.class);
            assertThat(testDato.dato).isEqualTo(ZonedDateTime.of(LocalDateTime.of(1947, 1, 26, 9, 50, 7).plus(838, MILLIS), DEFAULT_ZONE));
        }

        @Test
        public void localDateTimeIsNull() {
            TestLocalDateTime testDato = JsonUtils.fromJson("{\"dato\":null}", TestLocalDateTime.class);
            assertThat(testDato.dato).isNull();
        }

        @Test
        public void date() {
            String json = "{\"dato\":\"2021-01-26T09:50:07.838+01:00\"}";
            TestDate testDate = JsonUtils.fromJson(json, TestDate.class);
            Date expectedDate = Date.from(
                    ZonedDateTime.of(2021, 1, 26, 9, 50, 7, 0, DEFAULT_ZONE)
                            .plus(838, MILLIS).toInstant());
            assertThat(testDate.dato).isEqualTo(expectedDate);
            Assertions.assertThat(JsonUtils.toJson(testDate)).isEqualTo(json);
        }
    }

    @Nested
    class fromJsonArray {

        @Test
        public void arrayOfPrimitives() {
            Assertions.assertThat(JsonUtils.fromJsonArray("[1,2,3]", Integer.class)).isEqualTo(asList(1, 2, 3));
            Assertions.assertThat(JsonUtils.fromJsonArray(new ByteArrayInputStream("[1,2,3]".getBytes()), Integer.class)).isEqualTo(asList(1, 2, 3));
        }

        @Test
        public void arrayOfObjects() {
            Assertions.assertThat(JsonUtils.fromJsonArray("[{\"aString\":\"a\"},{\"aString\":\"b\"}]", TestObject.class)).isEqualTo(asList(
                    new TestObject().setAString("a"),
                    new TestObject().setAString("b")
            ));
        }

        @Test
        public void arrayOfArray() {
            Assertions.assertThat(JsonUtils.fromJsonArray("[[1,2],[3,4],[5,6]]", List.class)).isEqualTo(asList(
                    asList(1,2),
                    asList(3,4),
                    asList(5,6)
            ));
        }

    }

    @Data
    @Accessors(chain = true)
    static class TestObject {
        private String aString = "test";
        private EnEnum enEnum = EnEnum.ABC;
        private LocalDateTime date = LocalDateTime.of(2017, 8, 9,13,49,13).plus(816, MILLIS);
    }

    private enum EnEnum {
        ABC,
        DEF
    }

    private static class CircularObject {
        private CircularObject object = this;
    }


    private static class TestDate {
        private Date dato;
        public TestDate() { }
    }

    private static class TestLocalDate {
        private LocalDate dato;
        public TestLocalDate() { }
    }

    private static class TestLocalDateTime {
        private LocalDateTime dato;
        public TestLocalDateTime() { }
    }

    private static class TestZonedDateTime {
        private ZonedDateTime dato;
        public TestZonedDateTime() { }
    }
}
