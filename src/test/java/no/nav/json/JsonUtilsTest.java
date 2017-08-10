package no.nav.json;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.Value;
import org.junit.Test;

import java.util.Date;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.json.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;


public class JsonUtilsTest {

    public static final String TEST_OBJECT_JSON = "{\"aString\":\"test\",\"enEnum\":\"ABC\",\"date\":\"2017-08-09T13:49:13.816+02:00\"}";
    public static final String EMPTY_ENUM_VALUE_JSON = "{\"aString\":\"test\",\"enEnum\":\"\"}";
    public static final String ELDGAMMEL_DATE = "{\"date\":\"0201-09-08T23:09:21+00:09:21\"}";

    @Test
    public void toJson_null() {
        assertThat(toJson(null)).isNull();
    }

    @Test
    public void toJson_empty() {
        assertThat(toJson("")).isEqualTo("\"\"");
    }

    @Test
    public void toJson_string() {
        assertThat(toJson("a string")).isEqualTo("\"a string\"");
    }

    @Test
    public void toJson_object() {
        assertThat(toJson(new TestObject())).isEqualTo(TEST_OBJECT_JSON);
    }

    @Test
    public void fromJson_() {
        assertThat(fromJson(TEST_OBJECT_JSON, TestObject.class)).isEqualTo(new TestObject());
    }

    @Test
    public void fromJson_emptyEnumString_null() {
        TestObject testObject = fromJson(EMPTY_ENUM_VALUE_JSON, TestObject.class);
        assertThat(testObject.enEnum).isNull();
    }

    @Test
    public void fromJson_eldgammelDate() {
        assertThat(fromJson(ELDGAMMEL_DATE, TestObject.class).date).isEqualTo(new Date(-55802566800000L));
    }

    @Test(expected = JsonMappingException.class)
    public void toJson_circularObject() {
        toJson(new CircularObject());
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