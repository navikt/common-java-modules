package no.nav.metrics;

import java.util.Map;

class InfluxHandler {
    public static String createLineProtocolPayload(String metricName, Map<String, String> tags, Map<String, Object> fields, long metricTimestamp) {
        String tagsString = convertTagsToCSVString(tags);
        String fieldsString = convertFieldsToCSVString(fields);

        return String.format("%s,%s %s %d", metricName, tagsString, fieldsString, metricTimestamp);
    }

    private static String convertTagsToCSVString(Map<String, String> tags) {
        String tagString = tags.toString();
        tagString = tagString.replace(" ", "");
        tagString = tagString.substring(1, tagString.length() - 1);

        return tagString;
    }

    private static String convertFieldsToCSVString(Map<String, Object> fields) {
        StringBuilder fieldString = new StringBuilder();

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            String key = field.getKey();
            Object rawValue = field.getValue();
            Object value = rawValue instanceof String ? createStringValue(rawValue) : rawValue;

            fieldString.append("," + key + "=" + value);
        }

        return fieldString.substring(1);
    }

    private static String createStringValue(Object value) {
        return "\"" + value + "\"";
    }
}
