package no.nav.metrics.handlers;

import org.json.JSONObject;
import org.junit.Test;

import static no.nav.metrics.handlers.SensuHandler.createJSON;
import static org.junit.Assert.assertEquals;

public class SensuHandlerTest {

    @Test
    public void createJSONSetsTypeToMetric() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals("metric", jsonObject.get("type"));
    }

    @Test
    public void createJSONSetsStatusToZero() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals(0, jsonObject.get("status"));
    }

    @Test
    public void createJSONSetsHandlersToEvents() {
        JSONObject jsonObject = createJSON("test", "test");
        String handlers = jsonObject.get("handlers").toString();

        assertEquals("[\"events_nano\"]", handlers);
    }
}