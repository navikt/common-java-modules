package no.nav.common.metrics;

public class Event extends Metric<Event> {

    Event(MetricsClient metricsClient, String name) {
        super(metricsClient, name + ".event");
        addFieldToReport("value", 1);
    }

    @Override
    protected Event self() {
        return this;
    }

    @Override
    public Event report() {
        metricsClient.report(name, fields, tags, System.currentTimeMillis());
        return this;
    }
}