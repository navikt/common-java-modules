package no.nav.common.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
    private String metricName;
    private Map<String, String> tags;
    private Map<String, Object> values;
    private long timeStamp;

    @Before
    public void setUp() {
        // LinkedHashMap to preserve the order of the values when testing. We want this, so that
        // the strings created by createLineProtocolPayload always have the values in a constistent order.
        metricName = "metric";
        tags = new LinkedHashMap<>();
        values = new LinkedHashMap<>();
        timeStamp = 1111;
    }

    @Test
    public void lineProtocolFormatIsCreatedCorrectly() {
        tags.put("tag", "something");
        values.put("value", 0);

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag=something value=0 1111", payload);
    }

    @Test
    public void multipleTagsInLineProtocolPayloadAreCSVFormatted() {
        tags.put("tag1", "1");
        tags.put("tag2", "2");
        tags.put("tag3", "3");
        values.put("value", 0);

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag1=1,tag2=2,tag3=3 value=0 1111", payload);
    }

    @Test
    public void multipleValuesInLineProtocolPayloadAreCSVFormatted() {
        tags.put("tag", "something");
        values.put("value1", 1);
        values.put("value2", 2);
        values.put("value3", 3);

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag=something value1=1,value2=2,value3=3 1111", payload);
    }


    @Test
    public void enumValuesAreFormattedAsStrings() {
        tags.put("tag", "something");
        values.put("value1", TestEnum.ABC);
        values.put("value2", TestEnum.DEF);

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag=something value1=\"ABC\",value2=\"DEF\" 1111", payload);
    }

    @Test
    public void objectValuesAreFormattedAsStrings() {
        tags.put("tag", "something");
        values.put("value1", new TestObject());
        values.put("value2", new TestObject());

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag=something value1=\"test object formatted\",value2=\"test object formatted\" 1111", payload);
    }

    @Test
    public void lineProtocolPayloadHasQuotationMarksOnStringValues() {
        tags.put("tag", "something");
        values.put("value1", "0");
        values.put("value2", 0);

        String payload = Utils.createInfluxLineProtocolPayload(metricName, tags, values, timeStamp);

        assertEquals("metric,tag=something value1=\"0\",value2=0 1111", payload);
    }

    @Test
    public void linearInterpolationShouldReturnCorrectValues() {
        assertEquals(10, Utils.linearInterpolation(10, 20, 0F));
        assertEquals(20, Utils.linearInterpolation(10, 20, 1F));
        assertEquals(15, Utils.linearInterpolation(10, 20, 0.5F));

        assertEquals(10, Utils.linearInterpolation(10, 20, -0.1F));
        assertEquals(20, Utils.linearInterpolation(10, 20, 1.1F));
    }

    private enum TestEnum {
        ABC,
        DEF
    }

    private class TestObject {

        @Override
        public String toString() {
            return "test object formatted";
        }
    }

}
