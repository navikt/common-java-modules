package no.nav.metrics;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

class MetricsClient {
    private static final Logger logger = LoggerFactory.getLogger(MetricsClient.class);
    private final Map<String, String> tags = new HashMap<>();
    private final String tagsString;

    public MetricsClient() {
        addSystemPropertiesToTags();
        tagsString = convertTagsToCSVString();
    }

    private void addSystemPropertiesToTags() {
        tags.put("application", System.getProperty("applicationName"));
        tags.put("hostname", System.getProperty("node.hostname"));
        tags.put("environment", System.getProperty("environment.name"));
    }

    private String convertTagsToCSVString() {
        String tagString = tags.toString();
        tagString = tagString.replace(" ", "");
        tagString = tagString.substring(1, tagString.length() - 1);

        return tagString;
    }

    public void report(String metricName, Map<String, Object> fields, long timestampInSeconds) {
        JSONObject jsonObject = createSensuJSON(metricName, convertFieldsToCSVString(fields), timestampInSeconds);
        reportToSensu(jsonObject);
    }

    private String convertFieldsToCSVString(Map<String, Object> fields) {
        StringBuilder fieldString = new StringBuilder();

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            String key = field.getKey();
            Object rawValue = field.getValue();
            Object value = rawValue instanceof String ? createStringValue(rawValue) : rawValue;

            fieldString.append("," + key + "=" + value);
        }

        return fieldString.substring(1);
    }

    private String createStringValue(Object value) {
        return "\"" + value + "\"";
    }

    private JSONObject createSensuJSON(String metricName, String metricValue, long metricTimestamp) {
        String lineProtocolPayload = createLineProtocolPayload(metricName, metricValue, metricTimestamp);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", tags.get("application"));
        jsonObject.put("type", "metric");
        jsonObject.put("output", lineProtocolPayload);
        jsonObject.put("status", 0);
        jsonObject.put("handlers", new JSONArray("[events]"));

        return jsonObject;
    }

    private String createLineProtocolPayload(String metricName, String metricValue, long metricTimestamp) {
        return String.format("%s,%s %s %d", metricName, tagsString, metricValue, metricTimestamp);
    }

    private void reportToSensu(JSONObject jsonObject) {
        logger.debug("JSON object to be sent to socket:", jsonObject.toString());

        try (Socket socket = new Socket()) {
            connectToSocket(socket);
            writeToSocket(socket, jsonObject);
        } catch (UnknownHostException e) {
            logger.error("Unknown host", e);
        } catch (IOException e) {
            logger.error("Couldn't get I/O for socket", e);
        }
    }

    private void connectToSocket(Socket socket) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 3030); //TODO: Port skal være i en system property
        socket.connect(inetSocketAddress, 500);
    }

    private void writeToSocket(Socket socket, JSONObject jsonObject) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            writer.write(jsonObject.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Couldn't get I/O for writer", e);
        }
    }

}