package no.nav.apiapp.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Test;

import static no.nav.apiapp.util.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;


public class JsonUtilsTest {

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
        assertThat(toJson(new TestObject())).isEqualTo("{\"aString\":\"test\"}");
    }

    @Test(expected = JsonMappingException.class)
    public void toJson_circularObject() {
        toJson(new CircularObject());
    }

    private static class TestObject {
        private String aString = "test";
    }

    private static class CircularObject {
        private CircularObject object = this;
    }

}