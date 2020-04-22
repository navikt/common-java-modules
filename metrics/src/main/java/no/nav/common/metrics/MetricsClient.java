package no.nav.common.metrics;

import java.util.Map;

public interface MetricsClient {

    void report(Event event);

    void report(String eventName, Map<String, Object> fields, Map<String, String> tags, long timestampInMilliseconds);

}
