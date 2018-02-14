package no.nav.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.lang.System.setProperty;
import static no.nav.json.JsonProvider.createObjectMapper;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.sbl.util.EnvironmentUtils.ENVIRONMENT_CLASS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JsonUtilsTest {
    private static final String TEST_MAP_OF_MAP = "{\"app\": {\"key\": true}, \"app2\": {\"key2\": false}}";
    private static final String TEST_OBJECT_JSON = "{\"aString\":\"test\",\"enEnum\":\"ABC\",\"date\":\"2017-08-09T13:49:13.816+02:00\"}";
    private static final String TEST_OBJECT_PRETTY_JSON = "{" + lineSeparator()
            + "  \"aString\" : \"test\"," + lineSeparator()
            + "  \"enEnum\" : \"ABC\"," + lineSeparator()
            + "  \"date\" : \"2017-08-09T13:49:13.816+02:00\"" + lineSeparator()
            + "}";
    private static final String EMPTY_ENUM_VALUE_JSON = "{\"aString\":\"test\",\"enEnum\":\"\"}";

    // eldgamle datoer med sekund-offset skaper problemer for bl.a. moment js.
    // velger derfor å formattere gamle datoer uten offset
    private static final String ELDGAMMEL_DATE_MED_SAER_OFFSET = "{\"date\":\"0201-09-08T23:31:54+00:09:21\"}";
    private static final String ELDGAMMEL_DATE_MED_ZULU = "{\"date\":\"0201-09-08T23:22:33Z\"}";
    private static final String SERIALISERT_ELDGAMMEL_DATE = "\"0201-09-08T23:22:33Z\"";
    // ELDGAMMEL_DATE_MED_SAER_OFFSET -> SERIALISERT_ELDGAMMEL_DATE: minutter/sekunder endrer seg fordi vi flytter datoen fra sært offset til zulu
    // ELDGAMMEL_DATE_MED_ZULU -> SERIALISERT_ELDGAMMEL_DATE: minutter/sekunder endrer seg ikke



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
        public void inputstream() {
            assertThat(fromJson(new ByteArrayInputStream(TEST_OBJECT_JSON.getBytes()), TestObject.class)).isEqualTo(new TestObject());
        }

        @Test
        public void typereference() {
            TypeReference<Map<String, Map<String, Boolean>>> type = new TypeReference<Map<String, Map<String, Boolean>>>() {};
            Map<String, Map<String, Boolean>> map = fromJson(TEST_MAP_OF_MAP, type);

            assertThat(map.get("app").get("key")).isTrue();
            assertThat(map.get("app2").get("key2")).isFalse();
        }


        @Test
        public void emptyEnumString_null() {
            TestObject testObject = fromJson(EMPTY_ENUM_VALUE_JSON, TestObject.class);
            assertThat(testObject.enEnum).isNull();
        }

        @Test
        public void eldgammelDate() {
            Date date = fromJson(ELDGAMMEL_DATE_MED_SAER_OFFSET, TestObject.class).date;
            assertThat(date).isEqualTo(new Date(-55802565447000L));
            assertThat(toJson(date)).isEqualTo(SERIALISERT_ELDGAMMEL_DATE);
        }

        @Test
        public void eldgammelDateUtenOffset() {
            Date date = fromJson(ELDGAMMEL_DATE_MED_ZULU, TestObject.class).date;
            assertThat(date).isEqualTo(new Date(-55802565447000L));
            assertThat(toJson(date)).isEqualTo(SERIALISERT_ELDGAMMEL_DATE);
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