package no.nav.metrics;

import no.nav.metrics.handlers.InfluxHandler;
import no.nav.metrics.handlers.SensuHandler;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MetricsClient {
    private final Map<String, String> tags = new HashMap<>();

    public MetricsClient() {
        addSystemPropertiesToTags();
    }

    private void addSystemPropertiesToTags() {
        tags.put("application", System.getProperty("applicationName"));
        tags.put("hostname", System.getProperty("node.hostname"));
        tags.put("environment", System.getProperty("environment.name"));
    }

    void report(String metricName, Map<String, Object> fields, long timestampInSeconds) {
        String output = InfluxHandler.createLineProtocolPayload(metricName, tags, fields, timestampInSeconds);
        JSONObject jsonObject = SensuHandler.createJSON(tags.get("application"), output);
        SensuHandler.report(jsonObject);
    }
}