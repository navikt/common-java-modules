package no.nav.metrics;

public class Event extends Metric {

    Event(MetricsClient metricsClient, String name) {
        super(metricsClient, name + ".event");
        addFieldToReport("value", 1);
    }

    @Override
    public void report() {
        metricsClient.report(name, fields, tags, System.currentTimeMillis());
    }
}