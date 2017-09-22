package no.nav.json;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.lang.System.lineSeparator;
import static java.lang.System.setProperty;
import static no.nav.json.JsonProvider.createObjectMapper;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.sbl.util.EnvironmentUtils.ENVIRONMENT_CLASS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JsonUtilsTest {

    private static final String TEST_OBJECT_JSON = "{\"aString\":\"test\",\"enEnum\":\"ABC\",\"date\":\"2017-08-09T13:49:13.816+02:00\"}";
    private static final String TEST_OBJECT_PRETTY_JSON = "{" + lineSeparator()
            + "  \"aString\" : \"test\"," + lineSeparator()
            + "  \"enEnum\" : \"ABC\"," + lineSeparator()
            + "  \"date\" : \"2017-08-09T13:49:13.816+02:00\"" + lineSeparator()
            + "}";
    private static final String EMPTY_ENUM_VALUE_JSON = "{\"aString\":\"test\",\"enEnum\":\"\"}";
    private static final String ELDGAMMEL_DATE = "{\"date\":\"0201-09-08T23:09:21+00:09:21\"}";

    @Nested
    class toJson {
        @Test
        public void nullverdi() {
            assertThat(toJson(null)).isNull();
        }

        @Test
        public void empty() {
            assertThat(toJson("")).isEqualTo("\"\"");
        }

        @Test
        public void string() {
            assertThat(toJson("a string")).isEqualTo("\"a string\"");
        }

        @Test
        public void object() {
            assertThat(toJson(new TestObject())).isEqualTo(TEST_OBJECT_JSON);
        }

        @Test
        public void pretty_print_i_test() {
            setProperty(ENVIRONMENT_CLASS_PROPERTY_NAME, "t");
            assertThat(toJson(new TestObject(), createObjectMapper())).isEqualTo(TEST_OBJECT_PRETTY_JSON);
        }

        @Test
        public void circularObject() {
            assertThatThrownBy(() -> toJson(new CircularObject())).isInstanceOf(JsonMappingException.class);
        }
    }

    @Nested
    class fromJson {

        @Test
        public void objekt() {
            assertThat(fromJson(TEST_OBJECT_JSON, TestObject.class)).isEqualTo(new TestObject());
        }

        @Test
        public void emptyEnumString_null() {
            TestObject testObject = fromJson(EMPTY_ENUM_VALUE_JSON, TestObject.class);
            assertThat(testObject.enEnum).isNull();
        }

        @Test
        public void eldgammelDate() {
            assertThat(fromJson(ELDGAMMEL_DATE, TestObject.class).date).isEqualTo(new Date(-55802566800000L));
        }

    }


    @Value
    private static class TestObject {
        private String aString = "test";
        private EnEnum enEnum = EnEnum.ABC;
        private Date date = new Date(1502279353816L);
    }

    private enum EnEnum {
        ABC,
        DEF
    }

    private static class CircularObject {
        private CircularObject object = this;
    }

}